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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import android.net.Uri
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustFixture.Companion.VERIFIER_DNS
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests that [OpenId4VpManager] processes each request with the trust verdict for the certificate
 * chain of that request.
 *
 * The requests are resolved by the OpenID4VP library.
 */
class OpenId4VpManagerReaderTrustTest {

    private val fixture = ReaderTrustFixture()
    private val events = LinkedBlockingQueue<TransferEvent>()
    private val uri = mockk<Uri> { every { scheme } returns "openid4vp" }

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns uri
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `the verdict of a trusted request is not applied to the next request`() {
        val manager = manager(requestProcessor(ReaderAuthPolicy.AlwaysRequire(fixture.readerTrustStore)))

        manager.resolveRequestUri(
            signedRequestUri(fixture.verifierChain, fixture.verifierKey)
        )
        fixture.assertVerifierVerdict(receivedRequest())
        manager.resolveRequestUri(redirectUriRequestUri())

        val processed = receivedRequest()
        fixture.assertUnauthenticated(processed)
        assertTrue(fixture.isRejectedByPolicy(generateResponse(processed)))
    }

    @Test
    fun `an untrusted certificate chain does not cause the next request to be rejected`() {
        val manager = manager(requestProcessor(ReaderAuthPolicy.EnforceIfPresent(fixture.readerTrustStore)))

        // A certificate chain that is not trusted, signed with the key of its leaf certificate.
        manager.resolveRequestUri(
            signedRequestUri(fixture.attackerChain, fixture.verifierKey)
        )
        assertIs<TransferEvent.Error>(nextEvent())
        manager.resolveRequestUri(redirectUriRequestUri())

        val processed = receivedRequest()
        fixture.assertUnauthenticated(processed)
        assertFalse(fixture.isRejectedByPolicy(generateResponse(processed)))
    }

    @Test
    fun `a verdict recorded late by a cancelled resolution does not replace the verdict of the next request`() {
        val validating = CountDownLatch(1)
        val release = CountDownLatch(1)
        val validated = CountDownLatch(1)
        val blocked = AtomicBoolean(false)
        // Validates the first certificate chain after release.
        val readerTrustStore = object : ReaderTrustStore by fixture.readerTrustStore {
            override fun validateCertificationTrustPath(chainToDocumentSigner: List<X509Certificate>): Boolean {
                if (!blocked.compareAndSet(false, true)) {
                    return fixture.readerTrustStore.validateCertificationTrustPath(chainToDocumentSigner)
                }
                validating.countDown()
                release.await(20, TimeUnit.SECONDS)
                return fixture.readerTrustStore.validateCertificationTrustPath(chainToDocumentSigner)
                    .also { validated.countDown() }
            }
        }
        val cancelledEnded = CountDownLatch(1)
        // Runs while the next request is resolved, after its certificate chain was validated.
        // Meanwhile, the cancelled resolution completes the validation of its certificate chain.
        // The library applies this policy only to x509_hash clients with a verifier_info.
        val registrationCertificatePolicy = RegistrationCertificatePolicy { _, _, _ ->
            release.countDown()
            validated.await(20, TimeUnit.SECONDS)
            cancelledEnded.await(20, TimeUnit.SECONDS)
            RegistrationCertificatePolicy.Authorization.Granted()
        }
        val manager = manager(
            requestProcessor(ReaderAuthPolicy.EnforceIfPresent(readerTrustStore), readerTrustStore),
            registrationCertificatePolicy,
        )
        manager.addTransferEventListener {
            if (it is TransferEvent.Disconnected || it is TransferEvent.Error) cancelledEnded.countDown()
        }

        // The verifier's certificate with a certificate of the attacker as its issuer.
        manager.resolveRequestUri(signedRequestUri(fixture.attackerChain, fixture.verifierKey))
        assertTrue(validating.await(20, TimeUnit.SECONDS), "Expected the certificate chain to be validated")
        manager.resolveRequestUri(
            signedRequestUri(
                fixture.verifierChain,
                fixture.verifierKey,
                fixture.x509HashClientId,
                fixture.verifierInfo,
            )
        )

        val cancelled = nextEvent()
        assertTrue(cancelled is TransferEvent.Disconnected || cancelled is TransferEvent.Error)
        val processed = receivedRequest()
        fixture.assertVerifierVerdict(processed)
        assertFalse(fixture.isRejectedByPolicy(generateResponse(processed)))
    }

    private fun requestProcessor(
        readerAuthPolicy: ReaderAuthPolicy,
        readerTrustStore: ReaderTrustStore = fixture.readerTrustStore,
    ) = DcqlRequestProcessor(
        documentManager = fixture.documentManager(),
        readerTrustStore = readerTrustStore,
        readerAuthPolicy = readerAuthPolicy,
    )

    private fun manager(
        requestProcessor: DcqlRequestProcessor,
        registrationCertificatePolicy: RegistrationCertificatePolicy? = null,
    ) = OpenId4VpManager(
        config = fixture.config(),
        requestProcessor = requestProcessor,
        listenersExecutor = Executor { it.run() },
        registrationCertificatePolicy = registrationCertificatePolicy,
    ).apply { addTransferEventListener { events.put(it) } }

    /** A request of the verifier whose request object carries [x5c] and is signed with [signingKey]. */
    private fun signedRequestUri(
        x5c: List<X509Certificate>,
        signingKey: PrivateKey,
        clientId: String = fixture.x509SanDnsClientId,
        verifierInfo: JsonArray? = null,
    ): String {
        val claims = buildJsonObject {
            put("client_id", clientId)
            put("response_type", "vp_token")
            put("response_mode", "direct_post")
            put("response_uri", RESPONSE_URI)
            put("nonce", fixture.nonce())
            put("aud", "https://self-issued.me/v2")
            put("dcql_query", fixture.dcql)
            verifierInfo?.let { put("verifier_info", it) }
        }
        val requestObject = fixture.requestObject(claims, x5c, signingKey)
        return "openid4vp://?client_id=${encode(clientId)}&request=$requestObject"
    }

    /** An unsigned request of a verifier that is identified by its redirect URI. */
    private fun redirectUriRequestUri(): String = "openid4vp://?response_type=vp_token" +
        "&client_id=${encode("redirect_uri:$REDIRECT_URI_VERIFIER")}" +
        "&response_mode=direct_post" +
        "&response_uri=${encode(REDIRECT_URI_VERIFIER)}" +
        "&nonce=${encode(fixture.nonce())}" +
        "&dcql_query=${encode(fixture.dcql.toString())}"

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    private fun nextEvent(): TransferEvent = events.poll(20, TimeUnit.SECONDS) ?: fail("No transfer event")

    private fun receivedRequest(): RequestProcessor.ProcessedRequest.Success {
        val received = assertIs<TransferEvent.RequestReceived>(nextEvent())
        return assertIs<RequestProcessor.ProcessedRequest.Success>(received.processedRequest)
    }

    private fun generateResponse(processed: RequestProcessor.ProcessedRequest.Success) = runBlocking {
        processed.generateResponse(processed.presentmentSelections.first(), emptyMap())
    }

    private companion object {
        const val RESPONSE_URI = "https://$VERIFIER_DNS/response"
        const val REDIRECT_URI_VERIFIER = "https://other-verifier.example/response"
    }
}
