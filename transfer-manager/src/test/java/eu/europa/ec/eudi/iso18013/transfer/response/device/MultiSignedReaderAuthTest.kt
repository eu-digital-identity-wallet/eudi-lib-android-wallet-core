/*
 *  Copyright (c) 2025-2026 European Commission
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

import eu.europa.ec.eudi.iso18013.transfer.Context
import eu.europa.ec.eudi.iso18013.transfer.createDocumentManager
import eu.europa.ec.eudi.iso18013.transfer.mockAndroidLog
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Simple
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.request.buildDeviceRequest
import org.multipaz.mdoc.util.MdocUtil
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
 * Reader authentication when a request carries several `readerAuthAll` signatures
 * (ISO/IEC 18013-5 second edition).
 *
 * Each signature is a separate identity of the *same* reader, typically one per trust framework
 * it is registered in, so trust is disjunctive: the first identity that validates against the
 * configured trust store is the one the wallet acts on. These tests pin that selection rule —
 * in particular that a trusted identity is found even when an untrusted one is listed first.
 */
@OptIn(ExperimentalTime::class)
class MultiSignedReaderAuthTest {

    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setup() {
        mockLog = mockAndroidLog()
    }

    @After
    fun tearDown() {
        mockLog.close()
    }

    /** A reader identity: its own root, a leaf certificate under it, and the signing key. */
    private class ReaderIdentityFixture(val commonName: String) {
        lateinit var signingKey: AsymmetricKey.X509CertifiedExplicit

        suspend fun build(): ReaderIdentityFixture {
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
            val rootKey = AsymmetricKey.X509CertifiedExplicit(
                certChain = X509CertChain(listOf(rootCert)),
                privateKey = rootPrivateKey
            )

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
            signingKey = AsymmetricKey.X509CertifiedExplicit(
                certChain = X509CertChain(listOf(leafCert, rootCert)),
                privateKey = leafPrivateKey
            )
            return this
        }
    }

    /**
     * Builds a DeviceRequest for an mDL signed by every given identity in order, each contributing
     * one `readerAuthAll` signature over the whole request.
     */
    private suspend fun deviceRequestSignedBy(
        vararg identities: ReaderIdentityFixture
    ): TransferDeviceRequest {
        val sessionTranscript = buildCborArray {
            add(Simple.NULL); add(Simple.NULL); add(byteArrayOf(1, 2, 3))
        }
        val deviceRequest = buildDeviceRequest(sessionTranscript = sessionTranscript) {
            addDocRequest(
                docType = "org.iso.18013.5.1.mDL",
                nameSpaces = mapOf("org.iso.18013.5.1" to mapOf("given_name" to false))
            )
            identities.forEach { addReaderAuthAll(it.signingKey) }
        }
        return TransferDeviceRequest(
            deviceRequestBytes = Cbor.encode(deviceRequest.toDataItem()),
            sessionTranscriptBytes = Cbor.encode(sessionTranscript)
        )
    }

    /** A trust store that accepts a chain only when its leaf has one of [trustedCommonNames]. */
    private fun trustStoreAccepting(vararg trustedCommonNames: String): ReaderTrustStore =
        mockk<ReaderTrustStore>().also { store ->
            every { store.validateCertificationTrustPath(any()) } answers {
                @Suppress("UNCHECKED_CAST")
                val chain = firstArg<List<X509Certificate>>()
                val leafName = chain.firstOrNull()?.subjectX500Principal?.name.orEmpty()
                trustedCommonNames.any { leafName.contains("CN=$it") }
            }
            every { store.createCertificationTrustPath(any()) } returns null
        }

    private suspend fun process(
        request: TransferDeviceRequest,
        trustStore: ReaderTrustStore
    ): ProcessedDeviceRequest {
        val processor = DeviceRequestProcessor(
            documentManager = createDocumentManager(keyLockPassphrase = null),
            readerAuthPolicy = ReaderAuthPolicy.EnforceIfPresent(trustStore)
        )
        return assertIs<ProcessedDeviceRequest>(processor.process(request))
    }

    @Test
    fun `picks the trusted identity even when an untrusted one is signed first`() = runBlocking {
        val untrusted = ReaderIdentityFixture("Untrusted Reader").build()
        val trusted = ReaderIdentityFixture("Trusted Reader").build()

        val processed = process(
            request = deviceRequestSignedBy(untrusted, trusted),
            trustStore = trustStoreAccepting("Trusted Reader")
        )

        // Both signatures are carried through to the consumer...
        assertEquals(2, processed.requester.requesterIdentities.size)
        // ...but the trust verdict and display name come from the trusted one, not the first.
        val trustMetadata = assertNotNull(
            processed.trustMetadata,
            "A trusted identity is present, so the request must be reported as trusted"
        )
        assertEquals("Trusted Reader", trustMetadata.displayName)
    }

    @Test
    fun `picks the first identity when it is itself trusted`() = runBlocking {
        val first = ReaderIdentityFixture("First Reader").build()
        val second = ReaderIdentityFixture("Second Reader").build()

        val processed = process(
            request = deviceRequestSignedBy(first, second),
            trustStore = trustStoreAccepting("First Reader", "Second Reader")
        )

        assertEquals(2, processed.requester.requesterIdentities.size)
        assertEquals("First Reader", assertNotNull(processed.trustMetadata).displayName)
    }

    @Test
    fun `reports untrusted when no identity validates, while still carrying them all`() =
        runBlocking {
            val one = ReaderIdentityFixture("Reader One").build()
            val two = ReaderIdentityFixture("Reader Two").build()

            val processed = process(
                request = deviceRequestSignedBy(one, two),
                trustStore = trustStoreAccepting("Somebody Else")
            )

            assertNull(
                processed.trustMetadata,
                "No identity validates, so there is no trust metadata"
            )
            assertEquals(
                2,
                processed.requester.requesterIdentities.size,
                "Reader auth is still present and must remain visible to the policy"
            )
        }
}
