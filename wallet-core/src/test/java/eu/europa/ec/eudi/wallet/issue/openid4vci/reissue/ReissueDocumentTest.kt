/*
 * Copyright (c) 2026 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

import com.nimbusds.oauth2.sdk.`as`.ReadOnlyAuthorizationServerMetadata
import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuanceError
import eu.europa.ec.eudi.openid4vci.CredentialIssuerEndpoint
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.RefreshToken
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.openid4vci.TransactionId
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigners
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.DefaultOpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredContext
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssuerAuthorization
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssuerCreation
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceAuthorizationException
import eu.europa.ec.eudi.wallet.issue.openid4vci.toBytes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.ephemeral.EphemeralStorage
import java.io.IOException
import java.net.URI
import java.time.Instant
import java.util.Collections
import java.util.concurrent.Executor
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import com.nimbusds.oauth2.sdk.id.Issuer as AuthorizationServerId

/**
 * Covers [eu.europa.ec.eudi.wallet.issue.openid4vci.DefaultOpenId4VciManager.reissueDocument] past the token refresh: requesting the credential,
 * storing the new document and its metadata, and replacing the old one.
 *
 * The PID is re-issued as SD-JWT VC. It was issued together with the mdoc PID, so both records hold
 * the refresh token `rt-0`; refreshing it returns `at-1` / `rt-1`, and a new authorization returns
 * `at-fresh` / `rt-fresh`.
 */
class ReissueDocumentTest {

    private val storage = EphemeralStorage()

    private val newDocument = mockk<IssuedDocument>(relaxed = true) {
        every { id } returns NEW_DOCUMENT
        every { format } returns SdJwtVcFormat(VCT)
    }
    private val deferredDocument = mockk<DeferredDocument>(relaxed = true) {
        every { id } returns DEFERRED_DOCUMENT
    }
    private val unsignedDocument = mockk<UnsignedDocument>(relaxed = true) {
        every { id } returns UNSIGNED_DOCUMENT
        every { format } returns SdJwtVcFormat(VCT)
        // Credentials are paired with key aliases, so the document needs a signer to get one
        coEvery { getPoPSigners() } returns ProofOfPossessionSigners(
            listOf(mockk<ProofOfPossessionSigner> { every { keyAlias } returns NEW_KEY_ALIAS })
        )
    }
    private val documentManager = mockk<DocumentManager>(relaxed = true) {
        every { createDocument(any(), any(), any()) } returns Outcome.success(unsignedDocument)
        every { storeIssuedDocument(any(), any()) } returns Outcome.success(newDocument)
        every { storeDeferredDocument(any(), any()) } returns Outcome.success(deferredDocument)
    }

    /** Access tokens the credential requests were sent with, in order. */
    private val credentialRequestTokens = Collections.synchronizedList(mutableListOf<String>())

    /** How the credential endpoint answers a request sent with the given access token. */
    private var credentialEndpoint: (accessToken: String) -> Result<SubmissionOutcome> =
        { Result.success(issuedCredential()) }

    private val issuer = mockk<Issuer> {
        every { credentialOffer } returns pidCredentialOffer()
        coEvery { with(this@mockk) { any<AuthorizedRequest>().refresh() } } answers {
            val request = firstArg<AuthorizedRequest>()
            Result.success(request.withTokens(accessToken = "at-1", refreshToken = "rt-1"))
        }
        coEvery { with(this@mockk) { any<AuthorizedRequest>().request(any(), any()) } } answers {
            val request = firstArg<AuthorizedRequest>()
            val accessToken = request.accessToken.accessToken
            credentialRequestTokens.add(accessToken)
            credentialEndpoint(accessToken).map { outcome -> request to outcome }
        }
        every { with(this@mockk) { any<AuthorizedRequest>().deferredContext(any()) } } returns mockk(relaxed = true)
    }

    private val issuerAuthorization = mockk<IssuerAuthorization> {
        coEvery { authorize(any(), any()) } returns authorizedRequest(accessToken = "at-fresh", refreshToken = "rt-fresh")
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(DeferredContext::toBytes)
    }

    @Test
    fun `a successful re-issuance replaces the old document and stores the new one's metadata first`() =
        runBlocking {
            storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
            storeMetadata(PID_MDOC, refreshToken = "rt-0")

            val reissuance = reissue(PID_SD_JWT)

            val finished = assertIs<IssueEvent.Finished>(reissuance.end)
            assertEquals(listOf(NEW_DOCUMENT), finished.issuedDocuments)
            verify { documentManager.deleteDocumentById(PID_SD_JWT) }

            // The new document's metadata is in storage by the time Finished is delivered
            val newMetadata = assertNotNull(reissuance.newDocumentMetadataAtFinish)
            assertEquals("at-1", newMetadata.accessToken)
            assertEquals("rt-1", newMetadata.refreshToken)
            assertEquals(CONFIGURATION_ID, newMetadata.credentialConfigurationIdentifier)
            assertEquals(listOf(NEW_KEY_ALIAS), newMetadata.popKeyAliases)

            // The PID still holding the authorization gets the rotated token as well
            assertEquals("rt-1", loadMetadata(PID_MDOC)?.refreshToken)
            assertEquals(listOf("at-1"), credentialRequestTokens)
        }

    @Test
    fun `a deferred re-issuance keeps the old document until the deferred credential arrives`() = runBlocking {
        storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
        credentialEndpoint = { Result.success(SubmissionOutcome.Deferred(TransactionId("tx-1"), 5.seconds)) }
        var storedContext: DeferredContext? = null
        mockkStatic(DeferredContext::toBytes)
        every { any<DeferredContext>().toBytes() } answers {
            storedContext = firstArg()
            byteArrayOf(1)
        }

        val reissuance = reissue(PID_SD_JWT)

        val finished = assertIs<IssueEvent.Finished>(reissuance.end)
        assertEquals(listOf(DEFERRED_DOCUMENT), finished.issuedDocuments)
        assertIs<IssueEvent.DocumentDeferred>(reissuance.events.single { it is IssueEvent.DocumentDeferred })
        verify(exactly = 0) { documentManager.deleteDocumentById(PID_SD_JWT) }
        assertEquals(PID_SD_JWT, storedContext?.replacesDocumentId)
    }

    @Test
    fun `a successful re-issuance keeps the document it issued`() = runBlocking {
        storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
        // The issued document keeps the identifier of the unsigned document
        every { documentManager.getDocumentById(UNSIGNED_DOCUMENT) } returns newDocument

        assertIs<IssueEvent.Finished>(reissue(PID_SD_JWT).end)

        verify(exactly = 0) { documentManager.deleteDocumentById(UNSIGNED_DOCUMENT) }
    }

    @Test
    fun `a deferred re-issuance keeps the deferred document`() = runBlocking {
        storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
        credentialEndpoint = { Result.success(SubmissionOutcome.Deferred(TransactionId("tx-1"), 5.seconds)) }
        mockkStatic(DeferredContext::toBytes)
        every { any<DeferredContext>().toBytes() } returns byteArrayOf(1)
        // The deferred document keeps the identifier of the unsigned document
        every { documentManager.getDocumentById(UNSIGNED_DOCUMENT) } returns deferredDocument

        assertIs<IssueEvent.Finished>(reissue(PID_SD_JWT).end)

        verify(exactly = 0) { documentManager.deleteDocumentById(UNSIGNED_DOCUMENT) }
    }

    @Test
    fun `a rejected access token falls back to a new authorization when the user started the re-issuance`() =
        runBlocking {
            storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
            storeMetadata(PID_MDOC, refreshToken = "rt-0")
            credentialEndpoint = { accessToken ->
                if (accessToken == "at-fresh") Result.success(issuedCredential())
                else Result.failure(CredentialIssuanceError.InvalidToken())
            }

            val reissuance = reissue(PID_SD_JWT, allowAuthorizationFallback = true)

            assertIs<IssueEvent.Finished>(reissuance.end)
            assertEquals(listOf("at-1", "at-fresh"), credentialRequestTokens)
            coVerify(exactly = 1) { issuerAuthorization.authorize(issuer, null) }
            verify { documentManager.deleteDocumentById(PID_SD_JWT) }
            assertEquals("rt-fresh", reissuance.newDocumentMetadataAtFinish?.refreshToken)
            // The new authorization belongs to the new document only
            assertEquals("rt-1", loadMetadata(PID_MDOC)?.refreshToken)
        }

    @Test
    fun `a rejected access token fails a background re-issuance and keeps the old document`() = runBlocking {
        storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
        credentialEndpoint = { Result.failure(CredentialIssuanceError.InvalidToken()) }

        val reissuance = reissue(PID_SD_JWT, allowAuthorizationFallback = false)

        assertIs<ReissuanceAuthorizationException>(assertIs<IssueEvent.Failure>(reissuance.end).cause)
        coVerify(exactly = 0) { issuerAuthorization.authorize(any(), any()) }
        verify(exactly = 0) { documentManager.deleteDocumentById(PID_SD_JWT) }
    }

    @Test
    fun `a background re-issuance with rejected tokens deletes the document it created`() = runBlocking {
        storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
        credentialEndpoint = { Result.failure(CredentialIssuanceError.InvalidToken()) }
        every { documentManager.getDocumentById(UNSIGNED_DOCUMENT) } returns unsignedDocument

        reissue(PID_SD_JWT, allowAuthorizationFallback = false)

        verify(timeout = 5_000) { documentManager.deleteDocumentById(UNSIGNED_DOCUMENT) }
    }

    @Test
    fun `a re-issuance whose credential request fails deletes the document it created`() = runBlocking {
        storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
        credentialEndpoint = { Result.failure(IOException("Connection reset")) }
        every { documentManager.getDocumentById(UNSIGNED_DOCUMENT) } returns unsignedDocument

        val reissuance = reissue(PID_SD_JWT)

        assertIs<IssueEvent.Failure>(reissuance.end)
        verify(timeout = 5_000) { documentManager.deleteDocumentById(UNSIGNED_DOCUMENT) }
        verify(exactly = 0) { documentManager.deleteDocumentById(PID_SD_JWT) }
    }

    @Test
    fun `a rejected refresh token falls back to a new authorization that the other documents do not share`() =
        runBlocking {
            storeMetadata(PID_SD_JWT, refreshToken = "rt-0")
            storeMetadata(PID_MDOC, refreshToken = "rt-0")
            coEvery { with(issuer) { any<AuthorizedRequest>().refresh() } } returns
                Result.failure(CredentialIssuanceError.AccessTokenRequestFailed("invalid_grant", null))

            val reissuance = reissue(PID_SD_JWT, allowAuthorizationFallback = true)

            assertIs<IssueEvent.Finished>(reissuance.end)
            assertEquals(listOf("at-fresh"), credentialRequestTokens)
            coVerify(exactly = 1) { issuerAuthorization.authorize(issuer, null) }
            assertEquals("rt-fresh", reissuance.newDocumentMetadataAtFinish?.refreshToken)
            assertEquals("rt-0", loadMetadata(PID_MDOC)?.refreshToken)
        }

    @Test
    fun `re-issuing a document without stored metadata fails`() = runBlocking {
        val reissuance = reissue("unknown-document")

        assertIs<IllegalStateException>(assertIs<IssueEvent.Failure>(reissuance.end).cause)
        assertTrue(credentialRequestTokens.isEmpty())
        assertNull(loadMetadata(NEW_DOCUMENT))
    }

    private class Reissuance(
        val events: List<IssueEvent>,
        val end: IssueEvent,
        val newDocumentMetadataAtFinish: IssuanceMetadata?,
    )

    /**
     * Re-issues the document, answering the request for create settings, and waits for the event
     * that ends it.
     */
    private suspend fun reissue(documentId: String, allowAuthorizationFallback: Boolean = false): Reissuance {
        val manager = spyk(
            DefaultOpenId4VciManager(
                context = mockk(relaxed = true),
                documentManager = documentManager,
                walletAttestationKeyManager = mockk(),
                config = OpenId4VciManager.Config(
                    clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.None("client"),
                    authFlowRedirectionURI = "app://redirect",
                    dpopConfig = DPopConfig.Disabled,
                    issuanceMetadataStorage = storage,
                ),
            )
        ) {
            every { this@spyk.issuerCreator } returns mockk {
                coEvery { createIssuer(any<String>(), any(), any()) } returns IssuerCreation(
                    issuer,
                    null
                )
                every { clientAuthentication } returns ClientAuthentication.None("client")
                every { dpopKeyAlias } returns null
                every { clientAttestationPopKeyId } returns null
            }
            every { this@spyk.issuerAuthorization } returns this@ReissueDocumentTest.issuerAuthorization
        }

        val events = Collections.synchronizedList(mutableListOf<IssueEvent>())
        var newDocumentMetadataAtFinish: IssuanceMetadata? = null
        val end = CompletableDeferred<IssueEvent>()
        manager.reissueDocument(documentId, allowAuthorizationFallback, Executor { it.run() }) { event ->
            events.add(event)
            when (event) {
                is IssueEvent.DocumentRequiresCreateSettings.OptionalReusePolicy ->
                    event.resume(mockk<CreateDocumentSettings>(relaxed = true))

                is IssueEvent.Finished -> {
                    newDocumentMetadataAtFinish = runBlocking { loadMetadata(NEW_DOCUMENT) }
                    end.complete(event)
                }

                is IssueEvent.Failure -> end.complete(event)
                else -> Unit
            }
        }
        return Reissuance(events, withTimeout(10_000) { end.await() }, newDocumentMetadataAtFinish)
    }

    private fun pidCredentialOffer(): CredentialOffer {
        val issuerId = CredentialIssuerId(ISSUER).getOrThrow()
        val configurationId = CredentialConfigurationIdentifier(CONFIGURATION_ID)
        return mockk {
            every { credentialIssuerIdentifier } returns issuerId
            every { credentialConfigurationIdentifiers } returns listOf(configurationId)
            every { grants } returns null
            every { credentialIssuerMetadata } returns CredentialIssuerMetadata(
                credentialIssuerIdentifier = issuerId,
                credentialEndpoint = CredentialIssuerEndpoint("$ISSUER/credential").getOrThrow(),
                credentialConfigurationsSupported = mapOf(
                    configurationId to SdJwtVcCredential(credentialMetadata = null, type = VCT),
                ),
                issuerInfo = null,
            )
            every { authorizationServerMetadata } returns mockk<ReadOnlyAuthorizationServerMetadata> {
                every { issuer } returns AuthorizationServerId("https://auth.example.com")
                every { tokenEndpointURI } returns URI("https://auth.example.com/token")
            }
        }
    }

    private fun issuedCredential() = SubmissionOutcome.Success(
        credentials = listOf(IssuedCredential.string("issued-sd-jwt")),
        notificationId = null,
    )

    private fun authorizedRequest(accessToken: String, refreshToken: String) = AuthorizedRequest(
        accessToken = AccessToken.DPoP(accessToken, null),
        refreshToken = RefreshToken(refreshToken),
        credentialIdentifiers = emptyMap(),
        timestamp = Instant.ofEpochSecond(1800000000L),
        authorizationServerDpopNonce = null,
        resourceServerDpopNonce = null,
        grant = Grant.AuthorizationCode,
    )

    private fun AuthorizedRequest.withTokens(accessToken: String, refreshToken: String) = copy(
        accessToken = AccessToken.DPoP(accessToken, null),
        refreshToken = RefreshToken(refreshToken),
        timestamp = Instant.ofEpochSecond(1800000000L),
    )

    private suspend fun storeMetadata(documentId: String, refreshToken: String) {
        val metadata = IssuanceMetadata(
            credentialIssuerId = ISSUER,
            credentialConfigurationIdentifier = CONFIGURATION_ID,
            credentialEndpoint = "$ISSUER/credential",
            tokenEndpoint = "https://auth.example.com/token",
            authorizationServerId = "https://auth.example.com",
            clientId = "client",
            popKeyAliases = listOf("key-$documentId"),
            accessToken = "at-0",
            accessTokenType = "DPoP",
            refreshToken = refreshToken,
            tokenTimestamp = 1700000000L,
            grantType = "authorization_code",
        )
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
            .insert(key = documentId, data = ByteString(metadata.toByteArray()))
    }

    private suspend fun loadMetadata(documentId: String): IssuanceMetadata? =
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC).get(documentId)
            ?.let { IssuanceMetadata.fromByteArray(it.toByteArray()) }

    private companion object {
        const val ISSUER = "https://issuer.example.com"
        const val CONFIGURATION_ID = "eu.europa.ec.eudi.pid_vc_sd_jwt"
        const val VCT = "urn:eudi:pid:1"
        const val PID_SD_JWT = "pid-sd-jwt"
        const val PID_MDOC = "pid-mdoc"
        const val NEW_DOCUMENT = "new-pid-sd-jwt"
        const val DEFERRED_DOCUMENT = "deferred-pid-sd-jwt"
        const val NEW_KEY_ALIAS = "new-pid-key"
        const val UNSIGNED_DOCUMENT = "unsigned"
    }
}
