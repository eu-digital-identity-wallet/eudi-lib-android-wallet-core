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

package eu.europa.ec.eudi.iso18013.transfer.readerauth

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.mockAndroidLog
import eu.europa.ec.eudi.iso18013.transfer.readerauth.profile.ProfileValidation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import java.security.Security
import java.security.cert.CertPathValidator
import java.security.cert.CertPathValidatorException
import java.security.cert.CertStore
import java.security.cert.CertificateFactory
import java.security.cert.CollectionCertStoreParameters
import java.security.cert.PKIXParameters
import java.security.cert.PKIXRevocationChecker
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.util.Date
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderTrustStoreImplTest {

    private lateinit var mockLog: MockedStatic<Log>
    private lateinit var rootCertificate: X509Certificate
    private lateinit var leafCertificate: X509Certificate

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
        mockLog = mockAndroidLog()
        rootCertificate = trustedCertificate
        leafCertificate = validCertificate
    }

    @After
    fun close() {
        mockLog.close()
    }

    @Test
    fun testChainValidationWithNoCheckPolicy() {
        val certificateChain = listOf(leafCertificate, rootCertificate)

        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertTrue(result, "Certificate chain validation should succeed with NoCheck policy")
        verify(exactly = 1) {
            profileValidation.validate(any(), any())
        }
    }

    @Test
    fun testValidationReturnsFalseWhenProfileValidationFails() {
        val certificateChain = listOf(leafCertificate, rootCertificate)

        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns false

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertFalse(result, "Validation should fail when profile validation returns false")
    }

    @Test
    fun testValidationReturnsFalseForUntrustedChain() {
        val certificateChain = listOf(leafCertificate, rootCertificate)

        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true

        // Use an empty trust store — no trusted certificates
        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = emptyList(),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertFalse(result, "Validation should fail for untrusted certificate chain")
    }

    @Test
    fun testValidationReturnsFalseForEmptyChain() {
        val profileValidation = mockk<ProfileValidation>()

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        val result = trustStore.validateCertificationTrustPath(emptyList())

        assertFalse(result, "Validation should fail for empty chain")
    }

    @Test
    fun testDefaultRevocationPolicyIsHardFail() {
        val certificateChain = listOf(leafCertificate, rootCertificate)

        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true

        // Use constructor without explicit revocationPolicy — should default to HardFail
        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            errorLogger = { _, _, _ -> }
        )

        // With HardFail, validation should fail because the leaf cert has
        // a CRL distribution point (https://example.com/crl.pem) that is unreachable
        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertFalse(result, "Default policy (HardFail) should fail when CRL is unreachable")
    }

    @Test
    fun testHardFailPolicyFailsWhenCrlIsUnavailable() {
        val certificateChain = listOf(leafCertificate, rootCertificate)

        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.HardFail,
            errorLogger = { _, _, _ -> }
        )

        // HardFail should fail because the CRL at https://example.com/crl.pem is unreachable
        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertFalse(result, "HardFail policy should fail when CRL distribution point is unreachable")
    }

    @Test
    fun testSoftFailPolicySucceedsWhenCrlIsUnavailable() {
        val certificateChain = listOf(leafCertificate, rootCertificate)

        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.SoftFail,
            errorLogger = { _, _, _ -> }
        )

        // SoftFail should succeed because CRL unavailability is tolerated
        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertTrue(result, "SoftFail policy should succeed when CRL is unavailable")
    }

    @Test
    fun testRevokedCertificateIsRejectedWithCRL() {
        // This test validates that PKIXRevocationChecker correctly rejects a
        // certificate whose serial number appears in a CRL, using the same
        // checker configuration as ReaderTrustStoreImpl with HardFail policy.
        val certStore = CertStore.getInstance(
            "Collection",
            CollectionCertStoreParameters(listOf(rootCertificate, revokedCertificateCRL))
        )
        val trustAnchors = setOf(TrustAnchor(rootCertificate, null))

        val validator = CertPathValidator.getInstance("PKIX")
        val params = PKIXParameters(trustAnchors).apply {
            addCertStore(certStore)
            date = Date()
            isRevocationEnabled = true
            val checker = validator.revocationChecker as PKIXRevocationChecker
            checker.options = setOf(
                PKIXRevocationChecker.Option.PREFER_CRLS,
                PKIXRevocationChecker.Option.NO_FALLBACK,
            )
            addCertPathChecker(checker)
        }

        val certPath = CertificateFactory.getInstance("X.509")
            .generateCertPath(listOf(leafCertificate))

        // PKIXRevocationChecker should reject the certificate because it's in the CRL
        assertFailsWith<CertPathValidatorException> {
            validator.validate(certPath, params)
        }
    }

    @Test
    fun testGetDefaultFactoryWithRevocationPolicy() {
        val trustStore = ReaderTrustStore.getDefault(
            listOf(rootCertificate),
            RevocationPolicy.NoCheck
        )

        val certificateChain = listOf(leafCertificate, rootCertificate)
        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertTrue(result, "Factory-created trust store with NoCheck should validate successfully")
    }
}
