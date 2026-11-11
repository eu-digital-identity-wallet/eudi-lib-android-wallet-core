/*
 *  Copyright (c) 2026 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.iso18013.transfer.response.device

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.createDocumentManager
import eu.europa.ec.eudi.iso18013.transfer.mockAndroidLog
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationInfo
import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationValidator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.putCborArray
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.SignatureVerificationException
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.javaX509Certificates
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.mdoc.response.DeviceResponseParser
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialSelection
import org.multipaz.util.Constants
import java.security.cert.X509Certificate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest as TransferDeviceRequest

/**
 * Reader authentication when each DocRequest carries its own `readerAuth` (ISO/IEC 18013-5),
 * with requests signed by real reader keys and processed end to end.
 *
 * Each `readerAuth` covers only its own DocRequest, so a request is trusted only when every
 * DocRequest is signed and every signer is trusted, and reader authentication fails when any
 * signer is not trusted. The registration certificate is checked only against a trusted reader that
 * signs the whole request.
 */
@OptIn(ExperimentalTime::class)
class PerDocRequestReaderAuthTest {

    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setup() {
        mockLog = mockAndroidLog()
    }

    @After
    fun tearDown() {
        mockLog.close()
    }

    @Test
    fun `AlwaysRequire rejects a request with a trusted and an unsigned DocRequest`() = runBlocking {
        val trusted = reader(TRUSTED_A)

        for (parts in listOf(listOf(MDL to trusted, PID to null), listOf(MDL to null, PID to trusted))) {
            val outcome = present(request(parts), alwaysRequire())

            assertNull(outcome.trustedName)
            assertEquals(listOf(TRUSTED_A), outcome.identities)
            assertEquals(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR, outcome.status)
            assertEquals(emptyList(), outcome.sharedDocTypes)
        }
    }

    @Test
    fun `EnforceIfPresent shares a request with a trusted and an unsigned DocRequest without a trusted requester`() =
        runBlocking {
            val trusted = reader(TRUSTED_A)

            val outcome = present(request(listOf(MDL to trusted, PID to null)), enforceIfPresent())

            assertNull(outcome.trustedName)
            assertEquals(listOf(TRUSTED_A), outcome.identities)
            assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, outcome.status)
            assertEquals(listOf(MDL, PID), outcome.sharedDocTypes)
        }

    @Test
    fun `a DocRequest signed by an untrusted reader rejects the request wherever it is`() = runBlocking {
        val trusted = reader(TRUSTED_B)
        val untrusted = reader(UNTRUSTED_X)

        for (parts in listOf(listOf(MDL to trusted, PID to untrusted), listOf(MDL to untrusted, PID to trusted))) {
            for (policy in listOf(alwaysRequire(), enforceIfPresent())) {
                val outcome = present(request(parts), policy)

                assertNull(outcome.trustedName)
                assertEquals(2, outcome.identities.size)
                assertEquals(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR, outcome.status)
            }
        }
    }

    @Test
    fun `DocRequests signed by different trusted readers are trusted and report every signer`() = runBlocking {
        val first = reader(TRUSTED_A)
        val second = reader(TRUSTED_B)

        for (policy in listOf(alwaysRequire(), enforceIfPresent())) {
            val outcome = present(request(listOf(MDL to first, PID to second)), policy)

            assertEquals(TRUSTED_A, outcome.trustedName)
            assertEquals(listOf(TRUSTED_A, TRUSTED_B), outcome.identities)
            assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, outcome.status)
            assertEquals(listOf(MDL, PID), outcome.sharedDocTypes)
        }
    }

    @Test
    fun `DocRequests signed by the same reader report it once`() = runBlocking {
        val trusted = reader(TRUSTED_A)

        val outcome = present(request(listOf(MDL to trusted, PID to trusted)), alwaysRequire())

        assertEquals(TRUSTED_A, outcome.trustedName)
        assertEquals(listOf(TRUSTED_A), outcome.identities)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, outcome.status)
    }

    @Test
    fun `a request without reader authentication is rejected only by AlwaysRequire`() = runBlocking {
        val unsigned = request(listOf(MDL to null, PID to null))

        val required = present(unsigned, alwaysRequire())
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR, required.status)

        val ifPresent = present(unsigned, enforceIfPresent())
        assertNull(ifPresent.trustedName)
        assertEquals(emptyList(), ifPresent.identities)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, ifPresent.status)
        assertEquals(listOf(MDL, PID), ifPresent.sharedDocTypes)
    }

    @Test
    fun `a reader signature that does not verify fails processing`() = runBlocking {
        val trusted = reader(TRUSTED_A)
        // Signed for another session, so the signatures do not verify for this one.
        val signedForAnotherSession = request(
            parts = listOf(MDL to trusted, PID to trusted),
            signedTranscript = sessionTranscript(byteArrayOf(9, 9, 9)),
        )

        for (policy in listOf(ReaderAuthPolicy.DoNotEnforce(), enforceIfPresent(), alwaysRequire())) {
            assertSignatureFailure(signedForAnotherSession, policy)
        }
    }

    @Test
    fun `a reader signature that does not verify fails processing even when another one verifies`() =
        runBlocking {
            val trusted = reader(TRUSTED_A)
            val valid = Cbor.decode(request(listOf(MDL to trusted)).deviceRequestBytes)
            val invalid = Cbor.decode(
                request(listOf(PID to trusted), signedTranscript = sessionTranscript(byteArrayOf(9, 9, 9)))
                    .deviceRequestBytes
            )
            // The first DocRequest is signed for this session, the second for another one.
            val mixed = buildCborMap {
                put("version", "1.0")
                putCborArray("docRequests") {
                    add(valid["docRequests"].asArray.single())
                    add(invalid["docRequests"].asArray.single())
                }
            }

            assertSignatureFailure(
                TransferDeviceRequest(
                    deviceRequestBytes = Cbor.encode(mixed),
                    sessionTranscriptBytes = Cbor.encode(SESSION_TRANSCRIPT),
                ),
                enforceIfPresent(),
            )
        }

    @Test
    fun `EnforceIfPresent rejects a request whose readerAuthAll signer is not trusted`() = runBlocking {
        val untrusted = reader(UNTRUSTED_X)

        val outcome = present(request(listOf(MDL to null, PID to null), readerAuthAll = untrusted), enforceIfPresent())

        assertNull(outcome.trustedName)
        assertEquals(listOf(UNTRUSTED_X), outcome.identities)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR, outcome.status)
        assertEquals(emptyList(), outcome.sharedDocTypes)
    }

    @Test
    fun `with readerAuthAll only its signatures decide trust`() = runBlocking {
        val trusted = reader(TRUSTED_A)
        val untrusted = reader(UNTRUSTED_X)

        val outcome = present(
            request(listOf(MDL to untrusted, PID to null), readerAuthAll = trusted),
            enforceIfPresent(),
        )

        assertEquals(TRUSTED_A, outcome.trustedName)
        assertEquals(listOf(TRUSTED_A), outcome.identities)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, outcome.status)
        assertEquals(listOf(MDL, PID), outcome.sharedDocTypes)
    }

    @Test
    fun `the registration certificate is checked against the trusted reader that signs the whole request`() =
        runBlocking {
            val trusted = reader(TRUSTED_A)
            val untrusted = reader(UNTRUSTED_X)

            assertEquals(
                TRUSTED_A,
                registrationAccessCertificate(request(listOf(MDL to trusted, PID to trusted)), enforceIfPresent()),
            )
            assertEquals(
                TRUSTED_A,
                registrationAccessCertificate(
                    request(listOf(MDL to untrusted, PID to null), readerAuthAll = trusted),
                    enforceIfPresent(),
                ),
            )
        }

    @Test
    fun `the registration certificate is checked against no reader when no trusted reader signs the whole request`() =
        runBlocking {
            val trustedA = reader(TRUSTED_A)
            val trustedB = reader(TRUSTED_B)
            val untrusted = reader(UNTRUSTED_X)

            // An unsigned DocRequest.
            assertNull(
                registrationAccessCertificate(request(listOf(MDL to trustedA, PID to null)), enforceIfPresent())
            )
            // DocRequests signed by different readers.
            assertNull(
                registrationAccessCertificate(request(listOf(MDL to trustedA, PID to trustedB)), enforceIfPresent())
            )
            // A readerAuthAll signer that is not trusted.
            assertNull(
                registrationAccessCertificate(
                    request(listOf(MDL to null, PID to null), readerAuthAll = untrusted),
                    enforceIfPresent(),
                )
            )
            // DoNotEnforce without a trust source evaluates no trust.
            assertNull(
                registrationAccessCertificate(
                    request(listOf(MDL to trustedA, PID to trustedA)),
                    ReaderAuthPolicy.DoNotEnforce(),
                )
            )
        }

    private suspend fun assertSignatureFailure(request: TransferDeviceRequest, policy: ReaderAuthPolicy) {
        val processor = DeviceRequestProcessor(createDocumentManager(keyLockPassphrase = null), policy)
        val failure = assertIs<RequestProcessor.ProcessedRequest.Failure>(processor.process(request))
        assertIs<SignatureVerificationException>(failure.error)
    }

    /** What the user is shown for a request and what the wallet sends once the user accepts. */
    private class Outcome(
        val trustedName: String?,
        val identities: List<String>,
        val status: Long,
        val sharedDocTypes: List<String>,
    )

    /** Processes [request], accepts every offered document and generates the response. */
    private suspend fun present(request: TransferDeviceRequest, policy: ReaderAuthPolicy): Outcome {
        val processor = DeviceRequestProcessor(createDocumentManager(keyLockPassphrase = null), policy)
        val processed = assertIs<ProcessedDeviceRequest>(processor.process(request))
        val matches = processed.presentmentData.credentialSets.flatMap { set ->
            set.options.first().members.flatMap { member -> member.matches.take(1) }
        }
        val offeredDocTypes = matches.map { (it.source as CredentialMatchSourceIso18013).docRequest.docType }
        assertEquals(listOf(MDL, PID), offeredDocTypes)

        val result = assertIs<ResponseResult.Success>(processed.generateResponse(CredentialSelection(matches)))
        val response = assertIs<DeviceResponse>(result.response)
        val parsed = DeviceResponseParser(response.deviceResponseBytes, response.sessionTranscriptBytes).parse()
        return Outcome(
            trustedName = processed.trustMetadata?.displayName,
            identities = processed.requester.requesterIdentities.map { it.certChain.commonName() },
            status = parsed.status,
            sharedDocTypes = parsed.documents.map { it.docType },
        )
    }

    /**
     * Processes [request] and returns the common name of the access certificate that the
     * registration certificate is checked against, or null when the access chain is empty.
     */
    private suspend fun registrationAccessCertificate(
        request: TransferDeviceRequest,
        policy: ReaderAuthPolicy,
    ): String? {
        var accessChain: List<X509Certificate>? = null
        val validator = WrpRegistrationValidator { _, readerAccessChain, _ ->
            accessChain = readerAccessChain
            object : WrpRegistrationInfo {}
        }
        val processor = DeviceRequestProcessor(
            documentManager = createDocumentManager(keyLockPassphrase = null),
            readerAuthPolicy = policy,
            wrpRegistrationValidator = validator,
        )
        assertIs<ProcessedDeviceRequest>(processor.process(request))
        return assertNotNull(accessChain).firstOrNull()?.commonName()
    }

    /**
     * Builds a request with one DocRequest per entry of [parts], each signed by its reader or
     * unsigned when the reader is null, optionally followed by a `readerAuthAll` signature.
     */
    private suspend fun request(
        parts: List<Pair<String, AsymmetricKey.X509CertifiedExplicit?>>,
        readerAuthAll: AsymmetricKey.X509CertifiedExplicit? = null,
        signedTranscript: DataItem = SESSION_TRANSCRIPT,
    ): TransferDeviceRequest {
        val deviceRequest = buildDeviceRequest(sessionTranscript = signedTranscript) {
            for ((docType, signer) in parts) {
                addDocRequest(docType = docType, nameSpaces = nameSpaces(docType), readerKey = signer)
            }
            readerAuthAll?.let { addReaderAuthAll(it) }
        }
        return TransferDeviceRequest(
            deviceRequestBytes = Cbor.encode(deviceRequest.toDataItem()),
            sessionTranscriptBytes = Cbor.encode(SESSION_TRANSCRIPT),
        )
    }

    private fun nameSpaces(docType: String) = when (docType) {
        MDL -> mapOf("org.iso.18013.5.1" to mapOf("family_name" to false))
        PID -> mapOf(PID to mapOf("family_name" to false))
        else -> error("Unexpected docType $docType")
    }

    /** A reader key with a leaf certificate under its own root. */
    private suspend fun reader(commonName: String): AsymmetricKey.X509CertifiedExplicit {
        val validFrom = Clock.System.now() - 1.days
        val validUntil = Clock.System.now() + 365.days
        val rootPrivateKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val rootCert = MdocUtil.generateReaderRootCertificate(
            readerRootKey = AsymmetricKey.anonymous(rootPrivateKey),
            subject = X500Name.fromName("C=GR,CN=$commonName Root"),
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
            subject = X500Name.fromName("C=GR,CN=$commonName"),
            dnsName = null,
            serial = ASN1Integer.fromRandom(128),
            validFrom = validFrom,
            validUntil = validUntil
        )
        return AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(leafCert, rootCert)), leafPrivateKey)
    }

    private fun alwaysRequire() = ReaderAuthPolicy.AlwaysRequire(trustStore())

    private fun enforceIfPresent() = ReaderAuthPolicy.EnforceIfPresent(trustStore())

    /** A trust store that accepts a chain only when its leaf belongs to a trusted reader. */
    private fun trustStore(): ReaderTrustStore = mockk<ReaderTrustStore>().also { store ->
        every { store.validateCertificationTrustPath(any()) } answers {
            firstArg<List<X509Certificate>>().first().commonName() in setOf(TRUSTED_A, TRUSTED_B)
        }
        every { store.createCertificationTrustPath(any()) } returns null
    }

    private fun X509CertChain.commonName(): String = javaX509Certificates.first().commonName()

    private fun X509Certificate.commonName(): String =
        subjectX500Principal.name.split(",").first { it.startsWith("CN=") }.removePrefix("CN=")

    private companion object {
        const val MDL = "org.iso.18013.5.1.mDL"
        const val PID = "eu.europa.ec.eudi.pid.1"
        const val TRUSTED_A = "Trusted Reader A"
        const val TRUSTED_B = "Trusted Reader B"
        const val UNTRUSTED_X = "Untrusted Reader X"

        fun sessionTranscript(handover: ByteArray): DataItem = buildCborArray {
            add(Simple.NULL); add(Simple.NULL); add(handover)
        }

        val SESSION_TRANSCRIPT = sessionTranscript(byteArrayOf(1, 2, 3))
    }
}
