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
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.dcapi.DCAPIRequest
import eu.europa.ec.eudi.wallet.internal.makeOpenId4VPConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor
import eu.europa.ec.eudi.wallet.trust.ecLeafSignedByIntermediateCertificate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

/**
 * Tests that [OpenId4VpDCAPIRequestProcessor] fails the request of an X.509 client for which no
 * reader trust verdict was recorded while the request was resolved.
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
class OpenId4VpDCAPIRequestProcessorReaderAuthenticationTest {

    private val dcqlRequestProcessor = mockk<DcqlRequestProcessor>(relaxed = true)
    private val openId4Vp = mockk<OpenId4Vp.OverDcAPI>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic("androidx.credentials.registry.provider.ProviderGetCredentialRequest")
        mockkStatic(::makeOpenId4VPConfig)
        every { makeOpenId4VPConfig(any(), any()) } returns mockk()
        mockkObject(OpenId4Vp)
        every { OpenId4Vp.overDcApi(any()) } returns openId4Vp
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `a request of an x509 client without a verdict fails and is not processed`(): Unit = runBlocking {
        // The library resolves the request of an X.509 client without calling the reader trust.
        val resolved = mockk<ResolvedRequestObject>(relaxed = true) {
            every { client } returns Client.X509SanDns("verifier.example", ecLeafSignedByIntermediateCertificate)
            every { responseMode } returns ResponseMode.DCApi
        }
        coEvery { openId4Vp.resolveRequestObject(any(), any(), any()) } returns Resolution.Success(resolved)
        val processor = OpenId4VpDCAPIRequestProcessor(
            openId4VpConfig = OpenId4VpConfig.Builder()
                .withClientIdSchemes(ClientIdScheme.X509SanDns)
                .withSchemes("openid4vp")
                .withFormats(Format.MsoMdoc.ES256)
                .withEncryptionPolicy(OpenId4VpConfig.EncryptionPolicy { })
                .build(),
            dcqlRequestProcessor = dcqlRequestProcessor,
            privilegedAllowlist = "{}",
            supportedProtocols = listOf(DCAPIProtocol.OPENID4VP_V1_SIGNED),
        )

        val processed = processor.process(dcApiRequest())

        val failure = assertIs<RequestProcessor.ProcessedRequest.Failure>(processed)
        assertIs<IllegalStateException>(failure.error)
        coVerify(exactly = 0) { dcqlRequestProcessor.process(any()) }
    }

    /** A signed request of the verifier's origin; its content is resolved by the mocked library. */
    private fun dcApiRequest(): DCAPIRequest {
        val protocol = DCAPIProtocol.OPENID4VP_V1_SIGNED
        val json = buildJsonObject {
            putJsonArray("requests") {
                addJsonObject {
                    put("protocol", protocol.identifier)
                    put("data", buildJsonObject { put("request", "request-object") })
                }
            }
        }.toString()
        val option = mockk<GetDigitalCredentialOption> { every { requestJson } returns json }
        val appInfo = mockk<CallingAppInfo> { every { getOrigin(any()) } returns VERIFIER_ORIGIN }
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

    private companion object {
        const val VERIFIER_ORIGIN = "https://verifier.example"
    }
}
