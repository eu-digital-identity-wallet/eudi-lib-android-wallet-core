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

import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.CredentialIssuanceError
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.RefreshToken
import eu.europa.ec.eudi.wallet.issue.openid4vci.DefaultOpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssuerCreation
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssuerCreator
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceAuthorizationException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.ephemeral.EphemeralStorage
import java.time.Instant
import java.util.Collections
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Documents issued under one authorization (e.g. the PID as mso_mdoc and as SD-JWT VC) share its
 * refresh token. These tests check that a refresh rotating it, during the re-issuance of one of
 * them, leaves none of them holding the discarded token.
 *
 * The mocked issuer has no credential offer, so every re-issuance fails right after the refresh.
 * That is the case where the rotated token used to be lost.
 */
class ReissueDocumentTokenRotationTest {

    private val storage = EphemeralStorage()

    /** Refresh tokens sent to the token endpoint, in order. */
    private val sentRefreshTokens = Collections.synchronizedList(mutableListOf<String>())

    private val rotatingIssuer = mockk<Issuer> {
        coEvery { with(this@mockk) { any<AuthorizedRequest>().refresh() } } coAnswers {
            val request = firstArg<AuthorizedRequest>()
            val sent = request.refreshToken!!.refreshToken
            sentRefreshTokens.add(sent)
            delay(50)
            val next = sent.removePrefix("rt-").toInt() + 1
            Result.success(
                request.copy(
                    accessToken = AccessToken.DPoP("at-$next", null),
                    refreshToken = RefreshToken("rt-$next"),
                    timestamp = Instant.ofEpochSecond(1800000000L + next),
                )
            )
        }
    }

    @Test
    fun `a rotated refresh token is stored for every document of the authorization even when issuance then fails`() =
        runBlocking {
            store("mdoc", refreshToken = "rt-0")
            store("sd-jwt", refreshToken = "rt-0")
            store("mdl", refreshToken = "rt-100")

            val event = manager(rotatingIssuer).reissue("mdoc")

            assertIs<IssueEvent.Failure>(event)
            assertEquals("rt-1", load("mdoc").refreshToken)
            assertEquals("at-1", load("mdoc").accessToken)
            assertEquals("rt-1", load("sd-jwt").refreshToken)
            assertEquals("at-1", load("sd-jwt").accessToken)
            assertEquals("rt-100", load("mdl").refreshToken)
        }

    @Test
    fun `re-issuing the documents of an authorization one after another sends each refresh token once`() =
        runBlocking {
            store("mdoc", refreshToken = "rt-0")
            store("sd-jwt", refreshToken = "rt-0")
            val manager = manager(rotatingIssuer)

            manager.reissue("mdoc")
            manager.reissue("sd-jwt")

            assertEquals(listOf("rt-0", "rt-1"), sentRefreshTokens)
            assertEquals("rt-2", load("mdoc").refreshToken)
            assertEquals("rt-2", load("sd-jwt").refreshToken)
        }

    @Test
    fun `concurrent re-issuances of an authorization never send the same refresh token`() = runBlocking {
        store("mdoc", refreshToken = "rt-0")
        store("sd-jwt", refreshToken = "rt-0")

        // Separate managers, as an app creating one per call would have
        listOf("mdoc", "sd-jwt")
            .map { documentId -> async { manager(rotatingIssuer).reissue(documentId) } }
            .awaitAll()

        assertEquals(listOf("rt-0", "rt-1"), sentRefreshTokens)
        assertEquals("rt-2", load("mdoc").refreshToken)
        assertEquals("rt-2", load("sd-jwt").refreshToken)
    }

    @Test
    fun `a rejected refresh token without authorization fallback fails and keeps the stored tokens`() =
        runBlocking {
            store("mdoc", refreshToken = "rt-0")
            store("sd-jwt", refreshToken = "rt-0")
            val rejectingIssuer = mockk<Issuer> {
                coEvery { with(this@mockk) { any<AuthorizedRequest>().refresh() } } returns
                    Result.failure(CredentialIssuanceError.AccessTokenRequestFailed("invalid_grant", null))
            }

            val event = manager(rejectingIssuer).reissue("mdoc", allowAuthorizationFallback = false)

            assertIs<ReissuanceAuthorizationException>(assertIs<IssueEvent.Failure>(event).cause)
            assertEquals("rt-0", load("mdoc").refreshToken)
            assertEquals("rt-0", load("sd-jwt").refreshToken)
        }

    @Test
    fun `a refresh failing with another error is reported as is`() = runBlocking {
        store("mdoc", refreshToken = "rt-0")
        val serverError = CredentialIssuanceError.AccessTokenRequestFailed("server_error", null)
        val failingIssuer = mockk<Issuer> {
            coEvery { with(this@mockk) { any<AuthorizedRequest>().refresh() } } returns Result.failure(serverError)
        }

        val event = manager(failingIssuer).reissue("mdoc", allowAuthorizationFallback = true)

        assertEquals(serverError, assertIs<IssueEvent.Failure>(event).cause)
        assertEquals("rt-0", load("mdoc").refreshToken)
    }

    private fun manager(issuer: Issuer): DefaultOpenId4VciManager {
        val issuerCreator = mockk<IssuerCreator>(relaxed = true) {
            coEvery { createIssuer(any<String>(), any(), any()) } returns IssuerCreation(
                issuer,
                null
            )
        }
        return spyk(
            DefaultOpenId4VciManager(
                context = mockk(relaxed = true),
                documentManager = mockk(relaxed = true),
                walletAttestationKeyManager = mockk(),
                config = OpenId4VciManager.Config(
                    clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.None("client"),
                    authFlowRedirectionURI = "app://redirect",
                    dpopConfig = DPopConfig.Disabled,
                    issuanceMetadataStorage = storage,
                ),
            )
        ) {
            every { this@spyk.issuerCreator } returns issuerCreator
        }
    }

    /** Re-issues the document and returns the event that ended it. */
    private suspend fun DefaultOpenId4VciManager.reissue(
        documentId: String,
        allowAuthorizationFallback: Boolean = false,
    ): IssueEvent {
        val end = CompletableDeferred<IssueEvent>()
        reissueDocument(documentId, allowAuthorizationFallback, Executor { it.run() }) { event ->
            if (event is IssueEvent.Failure || event is IssueEvent.Finished) end.complete(event)
        }
        return withTimeout(10_000) { end.await() }
    }

    private suspend fun store(documentId: String, refreshToken: String) {
        val metadata = IssuanceMetadata(
            credentialIssuerId = "https://issuer.example.com",
            credentialConfigurationIdentifier = "eu.europa.ec.eudi.pid_$documentId",
            credentialEndpoint = "https://issuer.example.com/credential",
            tokenEndpoint = "https://auth.example.com/token",
            authorizationServerId = "https://auth.example.com",
            clientId = "wallet-client",
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

    private suspend fun load(documentId: String): IssuanceMetadata = IssuanceMetadata.fromByteArray(
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC).get(documentId)!!.toByteArray()
    )
}
