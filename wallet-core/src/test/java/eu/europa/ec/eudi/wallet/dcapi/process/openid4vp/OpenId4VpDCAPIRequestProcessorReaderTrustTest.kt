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

package eu.europa.ec.eudi.wallet.dcapi.process.openid4vp

import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.SelectedCredentialSet
import androidx.credentials.registry.provider.selectedCredentialSet
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.dcapi.DCAPIRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustFixture
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustFixture.Companion.VERIFIER_ORIGIN
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.PrivateKey
import java.security.cert.X509Certificate
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests that [OpenId4VpDCAPIRequestProcessor] processes each request with the trust verdict for the
 * certificate chain of that request.
 *
 * The requests are resolved by the OpenID4VP library.
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
class OpenId4VpDCAPIRequestProcessorReaderTrustTest {

    private val fixture = ReaderTrustFixture()

    @Before
    fun setUp() {
        mockkStatic("androidx.credentials.registry.provider.ProviderGetCredentialRequest")
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `the verdict of a trusted request is not applied to the next request`(): Unit = runBlocking {
        val processor = processor(ReaderAuthPolicy.AlwaysRequire(fixture.readerTrustStore))

        val trusted = processor.process(signedRequest(fixture.verifierChain, fixture.verifierKey))
        fixture.assertVerifierVerdict(assertIs<RequestProcessor.ProcessedRequest.Success>(trusted))
        val processed = processor.process(unsignedRequest())

        val success = assertIs<RequestProcessor.ProcessedRequest.Success>(processed)
        fixture.assertUnauthenticated(success)
        assertTrue(fixture.isRejectedByPolicy(generateResponse(success)))
    }

    @Test
    fun `an untrusted certificate chain does not cause the next request to be rejected`(): Unit = runBlocking {
        val processor = processor(ReaderAuthPolicy.EnforceIfPresent(fixture.readerTrustStore))

        // A certificate chain that is not trusted, signed with the key of its leaf certificate.
        val rejected = processor.process(signedRequest(fixture.attackerChain, fixture.verifierKey))
        assertIs<RequestProcessor.ProcessedRequest.Failure>(rejected)
        val processed = processor.process(unsignedRequest())

        val success = assertIs<RequestProcessor.ProcessedRequest.Success>(processed)
        fixture.assertUnauthenticated(success)
        assertFalse(fixture.isRejectedByPolicy(generateResponse(success)))
    }

    @Test
    fun `a verdict recorded while a request is resolved does not replace the verdict of that request`(): Unit =
        runBlocking {
            lateinit var processor: OpenId4VpDCAPIRequestProcessor
            var overlapped = false
            // Runs while the verifier's request is resolved, after its certificate chain was validated.
            // Meanwhile, another request carries the verifier's certificate with a certificate of the
            // attacker as its issuer.
            val registrationCertificatePolicy = RegistrationCertificatePolicy { _, _, _ ->
                if (!overlapped) {
                    overlapped = true
                    val attack = processor.process(signedRequest(fixture.attackerChain, fixture.verifierKey))
                    assertIs<RequestProcessor.ProcessedRequest.Failure>(attack)
                }
                RegistrationCertificatePolicy.Authorization.Granted()
            }
            processor = processor(
                ReaderAuthPolicy.EnforceIfPresent(fixture.readerTrustStore),
                registrationCertificatePolicy,
            )

            val processed = processor.process(
                signedRequest(
                    fixture.verifierChain,
                    fixture.verifierKey,
                    fixture.x509HashClientId,
                    fixture.verifierInfo,
                )
            )

            assertTrue(overlapped, "Expected the registration certificate policy to run")
            val success = assertIs<RequestProcessor.ProcessedRequest.Success>(processed)
            fixture.assertVerifierVerdict(success)
            assertFalse(fixture.isRejectedByPolicy(generateResponse(success)))
        }

    private fun processor(
        readerAuthPolicy: ReaderAuthPolicy,
        registrationCertificatePolicy: RegistrationCertificatePolicy? = null,
    ) = OpenId4VpDCAPIRequestProcessor(
        openId4VpConfig = fixture.config(),
        dcqlRequestProcessor = DcqlRequestProcessor(
            documentManager = fixture.documentManager(),
            readerTrustStore = fixture.readerTrustStore,
            readerAuthPolicy = readerAuthPolicy,
        ),
        privilegedAllowlist = "{}",
        supportedProtocols = listOf(DCAPIProtocol.OPENID4VP_V1_SIGNED, DCAPIProtocol.OPENID4VP_V1_UNSIGNED),
        registrationCertificatePolicy = registrationCertificatePolicy,
    )

    /** A request of the verifier's origin that carries [x5c] and is signed with [signingKey]. */
    private fun signedRequest(
        x5c: List<X509Certificate>,
        signingKey: PrivateKey,
        clientId: String = fixture.x509SanDnsClientId,
        verifierInfo: JsonArray? = null,
    ): DCAPIRequest {
        val claims = buildJsonObject {
            put("client_id", clientId)
            put("response_type", "vp_token")
            put("response_mode", "dc_api")
            put("nonce", fixture.nonce())
            put("dcql_query", fixture.dcql)
            putJsonArray("expected_origins") { add(VERIFIER_ORIGIN) }
            verifierInfo?.let { put("verifier_info", it) }
        }
        val data = buildJsonObject { put("request", fixture.requestObject(claims, x5c, signingKey)) }
        return dcApiRequest(DCAPIProtocol.OPENID4VP_V1_SIGNED, data, VERIFIER_ORIGIN)
    }

    /** An unsigned request of another origin. */
    private fun unsignedRequest(): DCAPIRequest {
        val data = buildJsonObject {
            put("response_type", "vp_token")
            put("response_mode", "dc_api")
            put("nonce", fixture.nonce())
            put("dcql_query", fixture.dcql)
        }
        return dcApiRequest(DCAPIProtocol.OPENID4VP_V1_UNSIGNED, data, OTHER_ORIGIN)
    }

    private fun dcApiRequest(protocol: DCAPIProtocol, data: JsonObject, origin: String): DCAPIRequest {
        val json = buildJsonObject {
            putJsonArray("requests") {
                addJsonObject {
                    put("protocol", protocol.identifier)
                    put("data", data)
                }
            }
        }.toString()
        val option = mockk<GetDigitalCredentialOption> { every { requestJson } returns json }
        val appInfo = mockk<CallingAppInfo> { every { getOrigin(any()) } returns origin }
        val selected = mockk<SelectedCredentialSet> {
            every { credentialSetId } returns "0 ${protocol.identifier}"
            every { credentials } returns emptyList()
        }
        val request = mockk<ProviderGetCredentialRequest> {
            every { credentialOptions } returns listOf(option)
            every { callingAppInfo } returns appInfo
        }
        every { request.selectedCredentialSet } returns selected
        return DCAPIRequest(request)
    }

    private suspend fun generateResponse(processed: RequestProcessor.ProcessedRequest.Success) =
        processed.generateResponse(processed.presentmentSelections.first(), emptyMap())

    private companion object {
        const val OTHER_ORIGIN = "https://other-verifier.example"
    }
}
