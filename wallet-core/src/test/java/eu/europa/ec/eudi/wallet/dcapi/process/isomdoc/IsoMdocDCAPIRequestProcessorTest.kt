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
package eu.europa.ec.eudi.wallet.dcapi.process.isomdoc

import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.SelectedCredentialSet
import androidx.credentials.registry.provider.selectedCredentialSet
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.wallet.dcapi.DCAPIException
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.dcapi.DCAPIRequest
import eu.europa.ec.eudi.wallet.dcapi.internal.getDCAPIIsoMdocSessionTranscript
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.SignatureVerificationException
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.mdoc.util.MdocUtil
import org.robolectric.RobolectricTestRunner
import kotlin.io.encoding.Base64
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalDigitalCredentialApi::class, ExperimentalTime::class)
@RunWith(RobolectricTestRunner::class)
class IsoMdocDCAPIRequestProcessorTest {

    @Before
    fun setUp() {
        mockkStatic("androidx.credentials.registry.provider.ProviderGetCredentialRequest")
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `a reader signature that does not verify fails with the SignatureVerificationException as cause`(): Unit =
        runBlocking {
            val processor = IsoMdocDCAPIRequestProcessor(
                documentManager = mockk(relaxed = true),
                readerAuthPolicy = ReaderAuthPolicy.DoNotEnforce(),
                privilegedAllowlist = "{}",
                zkSystemRepository = null,
            )

            val processed = processor.process(dcApiRequestSignedForAnotherOrigin())

            val failure = assertIs<RequestProcessor.ProcessedRequest.Failure>(processed)
            val error = assertIs<DCAPIException>(failure.error)
            assertIs<SignatureVerificationException>(error.cause)
        }

    /** An `org-iso-mdoc` request from [ORIGIN] whose DocRequest is signed for another origin. */
    private suspend fun dcApiRequestSignedForAnotherOrigin(): DCAPIRequest {
        val encryptionInfo = "encryption-info"
        val otherSessionTranscript = Cbor.decode(
            getDCAPIIsoMdocSessionTranscript(encryptionInfo, "https://other.example")
        )
        val deviceRequest = buildDeviceRequest(sessionTranscript = otherSessionTranscript) {
            addDocRequest(
                docType = "org.iso.18013.5.1.mDL",
                nameSpaces = mapOf("org.iso.18013.5.1" to mapOf("family_name" to false)),
                readerKey = readerKey(),
            )
        }
        val deviceRequestBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
            .encode(Cbor.encode(deviceRequest.toDataItem()))
        val requestJson = JSONObject().put(
            "requests",
            JSONArray().put(
                JSONObject()
                    .put("protocol", DCAPIProtocol.ISO_MDOC.identifier)
                    .put(
                        "data",
                        JSONObject()
                            .put("deviceRequest", deviceRequestBase64)
                            .put("encryptionInfo", encryptionInfo)
                    )
            )
        ).toString()
        val option = mockk<GetDigitalCredentialOption> { every { this@mockk.requestJson } returns requestJson }
        val appInfo = mockk<CallingAppInfo> { every { getOrigin(any()) } returns ORIGIN }
        val selected = mockk<SelectedCredentialSet> {
            every { credentialSetId } returns "0 ${DCAPIProtocol.ISO_MDOC.identifier}"
            every { credentials } returns emptyList()
        }
        val request = mockk<ProviderGetCredentialRequest> {
            every { credentialOptions } returns listOf(option)
            every { callingAppInfo } returns appInfo
        }
        every { request.selectedCredentialSet } returns selected
        return DCAPIRequest(request)
    }

    /** A reader key with a leaf certificate under its own root. */
    private suspend fun readerKey(): AsymmetricKey.X509CertifiedExplicit {
        val validFrom = Clock.System.now() - 1.days
        val validUntil = Clock.System.now() + 365.days
        val rootPrivateKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val rootCert = MdocUtil.generateReaderRootCertificate(
            readerRootKey = AsymmetricKey.anonymous(rootPrivateKey),
            subject = X500Name.fromName("C=GR,CN=Reader Root"),
            serial = ASN1Integer.fromRandom(128),
            validFrom = validFrom,
            validUntil = validUntil,
            crlUrl = "https://example.com/crl"
        )
        val rootKey = AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(rootCert)), rootPrivateKey)
        val leafPrivateKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val leafCert = MdocUtil.generateReaderCertificate(
            readerRootKey = rootKey,
            readerKey = leafPrivateKey.publicKey,
            subject = X500Name.fromName("C=GR,CN=Reader"),
            dnsName = null,
            serial = ASN1Integer.fromRandom(128),
            validFrom = validFrom,
            validUntil = validUntil
        )
        return AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(leafCert, rootCert)), leafPrivateKey)
    }

    private companion object {
        const val ORIGIN = "https://verifier.example"
    }
}
