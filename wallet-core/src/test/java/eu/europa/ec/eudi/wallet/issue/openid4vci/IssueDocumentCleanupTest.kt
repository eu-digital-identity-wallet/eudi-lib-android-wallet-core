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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.oauth2.sdk.`as`.ReadOnlyAuthorizationServerMetadata
import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerEndpoint
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigners
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.multipaz.crypto.Algorithm
import org.multipaz.storage.ephemeral.EphemeralStorage
import java.io.IOException
import java.net.URI
import java.time.Instant
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertIs
import com.nimbusds.oauth2.sdk.id.Issuer as AuthorizationServerId

/**
 * Covers [DefaultOpenId4VciManager.issueDocumentByOffer] when the issuance ends without storing the
 * document it created.
 */
class IssueDocumentCleanupTest {

    private val unsignedDocument = mockk<UnsignedDocument>(relaxed = true) {
        every { id } returns UNSIGNED_DOCUMENT
        every { format } returns SdJwtVcFormat(VCT)
        coEvery { getPoPSigners() } returns ProofOfPossessionSigners(
            listOf(mockk<ProofOfPossessionSigner> { every { keyAlias } returns "key" })
        )
    }
    private val documentManager = mockk<DocumentManager>(relaxed = true) {
        every { createDocument(any(), any(), any()) } returns Outcome.success(unsignedDocument)
        every { getDocumentById(UNSIGNED_DOCUMENT) } returns unsignedDocument
    }

    /** How the credential endpoint answers a credential request. */
    private var credentialEndpoint: () -> Result<SubmissionOutcome> = { error("not set") }

    private val issuer = mockk<Issuer> {
        every { credentialOffer } returns pidCredentialOffer()
        coEvery { with(this@mockk) { any<AuthorizedRequest>().request(any(), any()) } } answers {
            val request = firstArg<AuthorizedRequest>()
            credentialEndpoint().map { outcome -> request to outcome }
        }
    }

    @Test
    fun `an issuance whose credential request fails deletes the document it created`() = runBlocking {
        credentialEndpoint = { Result.failure(IOException("Connection reset")) }

        assertIs<IssueEvent.Failure>(issue())

        verify(timeout = 5_000) { documentManager.deleteDocumentById(UNSIGNED_DOCUMENT) }
    }

    @Test
    fun `an issuance whose user authentication is cancelled deletes the document it created`() = runBlocking {
        credentialEndpoint = {
            Result.failure(UserAuthRequiredException(Algorithm.ESP256, emptyMap(), resume = { error("not resumed") }))
        }

        assertIs<IssueEvent.Failure>(issue(cancelUserAuth = true))

        verify(timeout = 5_000) { documentManager.deleteDocumentById(UNSIGNED_DOCUMENT) }
    }

    /**
     * Issues the offered PID, answering the request for create settings, and waits for the event
     * that ends it.
     */
    private suspend fun issue(cancelUserAuth: Boolean = false): IssueEvent {
        val manager = spyk(
            DefaultOpenId4VciManager(
                context = mockk(relaxed = true),
                documentManager = documentManager,
                walletAttestationKeyManager = mockk(),
                config = OpenId4VciManager.Config(
                    clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.None("client"),
                    authFlowRedirectionURI = "app://redirect",
                    dpopConfig = DPopConfig.Disabled,
                    issuanceMetadataStorage = EphemeralStorage(),
                ),
            )
        ) {
            every { this@spyk.issuerCreator } returns mockk {
                coEvery { createIssuer(any<Offer>()) } returns IssuerCreation(issuer, null)
                every { clientAuthentication } returns ClientAuthentication.None("client")
                every { dpopKeyAlias } returns null
                every { clientAttestationPopKeyId } returns null
            }
            every { this@spyk.issuerAuthorization } returns mockk {
                coEvery { authorize(any(), any()) } returns authorizedRequest()
            }
        }

        val end = CompletableDeferred<IssueEvent>()
        manager.issueDocumentByOffer(Offer(issuer.credentialOffer), null, Executor { it.run() }) { event ->
            when (event) {
                is IssueEvent.DocumentRequiresCreateSettings.OptionalReusePolicy ->
                    event.resume(mockk<CreateDocumentSettings>(relaxed = true))

                is IssueEvent.DocumentRequiresUserAuth -> if (cancelUserAuth) event.cancel(null)
                is IssueEvent.Finished, is IssueEvent.Failure -> end.complete(event)
                else -> Unit
            }
        }
        return withTimeout(10_000) { end.await() }
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

    private fun authorizedRequest() = AuthorizedRequest(
        accessToken = AccessToken.DPoP("at", null),
        refreshToken = null,
        credentialIdentifiers = emptyMap(),
        timestamp = Instant.ofEpochSecond(1800000000L),
        authorizationServerDpopNonce = null,
        resourceServerDpopNonce = null,
        grant = Grant.AuthorizationCode,
    )

    private companion object {
        const val ISSUER = "https://issuer.example.com"
        const val CONFIGURATION_ID = "eu.europa.ec.eudi.pid_vc_sd_jwt"
        const val VCT = "urn:eudi:pid:1"
        const val UNSIGNED_DOCUMENT = "unsigned"
    }
}
