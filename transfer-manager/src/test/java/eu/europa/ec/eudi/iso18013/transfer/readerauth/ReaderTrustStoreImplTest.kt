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
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.bouncycastle.asn1.x509.DistributionPointName
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.IssuingDistributionPoint
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import java.io.IOException
import java.security.Security
import java.security.cert.CRLException
import java.security.cert.CertPathValidator
import java.security.cert.CertPathValidatorException
import java.security.cert.CertStore
import java.security.cert.CertificateFactory
import java.security.cert.CollectionCertStoreParameters
import java.security.cert.PKIXParameters
import java.security.cert.PKIXRevocationChecker
import java.security.cert.TrustAnchor
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class ReaderTrustStoreImplTest {

    private lateinit var mockLog: MockedStatic<Log>
    private lateinit var rootCertificate: X509Certificate
    private lateinit var leafCertificate: X509Certificate
    private val offline = CrlCache(fetch = { throw IOException("offline") })

    @Before
    fun setup() {
        // Other tests in this JVM insert BouncyCastle first, whose PKIX ignores SOFT_FAIL.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
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
    fun testChainIsValidatedUpToAnIntermediateTrustedCertificate() {
        val (leaf, intermediate, root) = loadValidThreeCertChain()

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(intermediate),
            profileValidation = { _, _ -> true },
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        assertTrue(trustStore.validateCertificationTrustPath(listOf(leaf, intermediate)))
        assertTrue(trustStore.validateCertificationTrustPath(listOf(leaf, intermediate, root)))
    }

    @Test
    fun testChainIsValidatedUpToTheFirstTrustedCertificate() {
        val (leaf, intermediate, root) = loadValidThreeCertChain()

        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(root, intermediate),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        assertTrue(trustStore.validateCertificationTrustPath(listOf(leaf, intermediate, root)))
        verify(exactly = 1) { profileValidation.validate(listOf(leaf), intermediate) }
    }

    @Test
    fun testTrustedSelfSignedSignerCertificateIsValidated() {
        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        assertTrue(trustStore.validateCertificationTrustPath(listOf(rootCertificate)))
        verify(exactly = 1) { profileValidation.validate(listOf(rootCertificate), rootCertificate) }
    }

    @Test
    fun testTrustedSelfSignedSignerCertificateIsNotCheckedForRevocation() {
        for (policy in listOf(RevocationPolicy.SoftFail, RevocationPolicy.HardFail)) {
            val logged = mutableListOf<String>()
            val trustStore = ReaderTrustStoreImpl(
                trustedCertificates = listOf(rootCertificate),
                profileValidation = { _, _ -> true },
                revocationPolicy = policy,
                errorLogger = { _, message, _ -> logged += message },
                crlCache = offline,
            )

            assertTrue(trustStore.validateCertificationTrustPath(listOf(rootCertificate)), "$policy")
            assertEquals(emptyList(), logged, "$policy")
        }
    }

    @Test
    fun testChainIsRejectedWhenTheSignerIsNotIssuedByTheFirstTrustedCertificate() {
        val (_, intermediate, _) = loadValidThreeCertChain()

        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(intermediate),
            profileValidation = { _, _ -> true },
            revocationPolicy = RevocationPolicy.NoCheck,
            errorLogger = { _, _, _ -> }
        )

        assertFalse(trustStore.validateCertificationTrustPath(listOf(leafCertificate, intermediate)))
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
            errorLogger = { _, _, _ -> },
            crlCache = offline,
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
            errorLogger = { _, _, _ -> },
            crlCache = offline,
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
            errorLogger = { _, _, _ -> },
            crlCache = offline,
        )

        // SoftFail should succeed because CRL unavailability is tolerated
        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertTrue(result, "SoftFail policy should succeed when CRL is unavailable")
    }

    @Test
    fun testCertificateListedByItsDistributionPointsCrlIsRejected() {
        for (policy in listOf(RevocationPolicy.SoftFail, RevocationPolicy.HardFail)) {
            assertFalse(validateWithCrl(policy, revokedCertificateCRL), "$policy should reject a revoked certificate")
        }
    }

    @Test
    fun testCertificateMissingFromItsDistributionPointsCrlIsTrusted() {
        assertTrue(validateWithCrl(RevocationPolicy.HardFail, emptyCRL))
    }

    @Test
    fun testCrlWithAForgedSignatureIsNotUsed() {
        assertFalse(validateWithCrl(RevocationPolicy.HardFail, forgedEmptyCRL))
    }

    @Test
    fun testCrlScopedToTheCertificatesDistributionPointIsUsed() {
        val scope = IssuingDistributionPoint(fullName(VALID_CERTIFICATE_CRL_URI), true, false, null, false, false)

        assertTrue(validateWithCrl(RevocationPolicy.HardFail, scopedCrl(scope)))
        assertFalse(validateWithCrl(RevocationPolicy.HardFail, scopedCrl(scope, revoked = listOf(leafCertificate))))
    }

    @Test
    fun testCrlScopedWithoutADistributionPointIsUsed() {
        val scope = IssuingDistributionPoint(null, true, false, null, false, false)

        assertTrue(validateWithCrl(RevocationPolicy.HardFail, scopedCrl(scope)))
    }

    private fun validateWithCrl(policy: RevocationPolicy, crl: X509CRL): Boolean {
        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true
        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(rootCertificate),
            profileValidation = profileValidation,
            errorLogger = { _, _, _ -> },
            revocationPolicy = policy,
            crlCache = CrlCache(fetch = { crl }),
        )
        return trustStore.validateCertificationTrustPath(listOf(leafCertificate, rootCertificate))
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
    fun testValidationFailsWhenItExceedsTheTimeout() {
        // Stands in for a CRL download that never completes, which the JVM PKIX provider
        // does not attempt by default.
        val hangingValidator = mockk<CertPathValidator>()
        every { hangingValidator.validate(any(), any()) } answers {
            Thread.sleep(10_000)
            error("unreachable")
        }
        mockkStatic(CertPathValidator::class)
        every { CertPathValidator.getInstance("PKIX") } returns hangingValidator
        try {
            val logged = mutableListOf<String>()
            val trustStore = ReaderTrustStoreImpl(
                trustedCertificates = listOf(rootCertificate),
                profileValidation = mockk(),
                errorLogger = { _, message, _ -> logged += message },
                revocationPolicy = RevocationPolicy.NoCheck,
                revocationCheckTimeout = 500.milliseconds,
            )

            val start = System.nanoTime()
            val result = trustStore.validateCertificationTrustPath(listOf(leafCertificate, rootCertificate))
            val elapsed = (System.nanoTime() - start).nanoseconds

            assertFalse(result, "Validation should fail once the timeout elapses")
            assertTrue(elapsed < 5.seconds, "validation took $elapsed")
            assertEquals(listOf("CERTIFICATE_PATH_TIMEOUT"), logged)
        } finally {
            unmockkStatic(CertPathValidator::class)
        }
    }

    @Test
    fun testSoftFailValidatesWithoutRevocationWhenItExceedsTheTimeout() {
        val logged = mutableListOf<String>()
        val start = System.nanoTime()
        val result = validateWithStalledRevocationCheck(crlCache = null, logged)
        val elapsed = (System.nanoTime() - start).nanoseconds

        assertTrue(result, "SoftFail should tolerate a stalled revocation check")
        assertTrue(elapsed < 5.seconds, "validation took $elapsed")
        assertEquals(listOf("CERTIFICATE_PATH_TIMEOUT_SOFT_FAIL"), logged)
    }

    @Test
    fun testSoftFailRejectsACertificateListedByACachedCrlWhenItExceedsTheTimeout() {
        val logged = mutableListOf<String>()

        assertFalse(validateWithStalledRevocationCheck(cacheHolding(revokedCertificateCRL), logged))
        assertEquals(listOf("CERTIFICATE_PATH_TIMEOUT_SOFT_FAIL", "CERTIFICATE_PATH_ERROR"), logged)
    }

    @Test
    fun testSoftFailIgnoresAnExpiredCachedCrlWhenItExceedsTheTimeout() {
        var now = Instant.now()
        var available = true
        val cache = CrlCache(clock = { now }, fetch = { if (available) revokedCertificateCRL else throw IOException("offline") })
        cache.crlsFor(listOf(leafCertificate))
        available = false
        now += 31.days.toJavaDuration()

        assertTrue(validateWithStalledRevocationCheck(cache, mutableListOf()))
    }

    @Test
    fun testSoftFailIgnoresACachedCrlWithAForgedSignatureWhenItExceedsTheTimeout() {
        assertTrue(validateWithStalledRevocationCheck(cacheHolding(forgedRevokedCertificateCRL), mutableListOf()))
    }

    @Test
    fun testSoftFailIgnoresAFutureRevocationInACachedCrlWhenItExceedsTheTimeout() {
        val crl = crlIssuedAt(Date(System.currentTimeMillis() - 60_000), listOf(leafCertificate), revokedAt = Date(System.currentTimeMillis() + 86_400_000))

        assertTrue(validateWithStalledRevocationCheck(cacheHolding(crl), mutableListOf()))
    }

    @Test
    fun testNoDistributionPointOfAnUntrustedChainIsContacted() {
        val contacted = mutableListOf<java.net.URI>()
        val profileValidation = mockk<ProfileValidation>()
        every { profileValidation.validate(any(), any()) } returns true
        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(certificateWithTwoDistributionPoints),
            profileValidation = profileValidation,
            errorLogger = { _, _, _ -> },
            revocationPolicy = RevocationPolicy.HardFail,
            crlCache = CrlCache(fetch = { contacted += it; revokedCertificateCRL }),
        )

        assertFalse(trustStore.validateCertificationTrustPath(listOf(leafCertificate, rootCertificate)))
        assertEquals(emptyList(), contacted)
    }

    // The cache is filled before the validation, which then finds it fresh and does not download.
    private fun cacheHolding(crl: X509CRL): CrlCache =
        CrlCache(fetch = { crl }).also { it.crlsFor(listOf(leafCertificate)) }

    private fun validateWithStalledRevocationCheck(crlCache: CrlCache?, logged: MutableList<String>): Boolean {
        val offlineValidator = CertPathValidator.getInstance("PKIX")
        val hangingValidator = mockk<CertPathValidator>()
        every { hangingValidator.revocationChecker } returns offlineValidator.revocationChecker
        every { hangingValidator.validate(any(), any()) } answers {
            Thread.sleep(10_000)
            error("unreachable")
        }
        mockkStatic(CertPathValidator::class)
        every { CertPathValidator.getInstance("PKIX") } returnsMany listOf(hangingValidator, offlineValidator)
        try {
            val profileValidation = mockk<ProfileValidation>()
            every { profileValidation.validate(any(), any()) } returns true
            val trustStore = ReaderTrustStoreImpl(
                trustedCertificates = listOf(rootCertificate),
                profileValidation = profileValidation,
                errorLogger = { _, message, _ -> logged += message },
                revocationPolicy = RevocationPolicy.SoftFail,
                revocationCheckTimeout = 500.milliseconds,
                crlCache = crlCache,
            )
            return trustStore.validateCertificationTrustPath(listOf(leafCertificate, rootCertificate))
        } finally {
            unmockkStatic(CertPathValidator::class)
        }
    }

    @Test
    fun testSoftFailRejectsACertificateListedByACachedCrlWhenTheCrlIsUnreadable() {
        assertFalse(
            validateWithUnreadableCrl(
                RevocationPolicy.SoftFail,
                expectedLog = listOf("CERTIFICATE_PATH_CRL_UNREADABLE_SOFT_FAIL", "CERTIFICATE_PATH_ERROR"),
                crlCache = cacheHolding(revokedCertificateCRL),
            ),
        )
    }

    @Test
    fun testSoftFailValidatesWithoutRevocationWhenTheCrlIsUnreadable() {
        assertTrue(validateWithUnreadableCrl(RevocationPolicy.SoftFail, expectedLog = listOf("CERTIFICATE_PATH_CRL_UNREADABLE_SOFT_FAIL")))
    }

    @Test
    fun testHardFailRejectsAnUnreadableCrl() {
        assertFalse(validateWithUnreadableCrl(RevocationPolicy.HardFail, expectedLog = listOf("CERTIFICATE_PATH_ERROR")))
    }

    @Test
    fun testSoftFailValidatesWithoutRevocationWhenTheCacheHasNoCrl() {
        val logged = mutableListOf<String>()

        assertTrue(validateWithoutPlatformCrlDownload(RevocationPolicy.SoftFail, logged))
        assertEquals(listOf("CERTIFICATE_PATH_CRL_UNAVAILABLE_SOFT_FAIL"), logged)
    }

    @Test
    fun testHardFailRejectsWhenTheCacheHasNoCrl() {
        val logged = mutableListOf<String>()

        assertFalse(validateWithoutPlatformCrlDownload(RevocationPolicy.HardFail, logged))
        assertEquals(listOf("CERTIFICATE_PATH_ERROR"), logged)
    }

    @Test
    fun testCrlCoveringOnlyPartOfTheRevocationsIsNotEnoughToSkipTheDownload() {
        for (crl in listOf(partitionedEmptyCRL, deltaEmptyCRL)) {
            val logged = mutableListOf<String>()

            assertTrue(validateWithoutPlatformCrlDownload(RevocationPolicy.SoftFail, logged, CrlCache(fetch = { crl })))
            assertEquals(listOf("CERTIFICATE_PATH_CRL_UNAVAILABLE_SOFT_FAIL"), logged)
        }
    }

    @Test
    fun testCrlWithoutNextUpdateIsNotEnoughToSkipTheDownload() {
        val logged = mutableListOf<String>()

        assertTrue(
            validateWithoutPlatformCrlDownload(RevocationPolicy.SoftFail, logged, CrlCache(fetch = { emptyCRLWithoutNextUpdate }))
        )
        assertEquals(listOf("CERTIFICATE_PATH_CRL_UNAVAILABLE_SOFT_FAIL"), logged)
    }

    @Test
    fun testCrlThatMayNotCoverTheCertificateIsNotEnoughToSkipTheDownload() {
        val uri = fullName(VALID_CERTIFICATE_CRL_URI)
        val crls = mapOf(
            "another distribution point" to
                scopedCrl(IssuingDistributionPoint(fullName("https://example.com/other.pem"), true, false, null, false, false)),
            "a distribution point without a URI" to scopedCrl(
                IssuingDistributionPoint(
                    DistributionPointName(GeneralNames(GeneralName(GeneralName.dNSName, "example.com"))),
                    true, false, null, false, false,
                )
            ),
            "CA certificates only" to scopedCrl(IssuingDistributionPoint(uri, false, true, null, false, false)),
            "an indirect CRL" to scopedCrl(IssuingDistributionPoint(uri, false, false, null, true, false)),
            "attribute certificates only" to scopedCrl(IssuingDistributionPoint(uri, false, false, null, false, true)),
        )
        for ((case, crl) in crls) {
            val logged = mutableListOf<String>()

            assertTrue(validateWithoutPlatformCrlDownload(RevocationPolicy.SoftFail, logged, CrlCache(fetch = { crl })), case)
            assertEquals(listOf("CERTIFICATE_PATH_CRL_UNAVAILABLE_SOFT_FAIL"), logged, case)
        }
    }

    // On a device, a revocation-checking PKIX validation downloads the CRLs it is not given.
    private fun validateWithoutPlatformCrlDownload(
        policy: RevocationPolicy,
        logged: MutableList<String>,
        crlCache: CrlCache = offline,
    ): Boolean {
        val offlineValidator = CertPathValidator.getInstance("PKIX")
        val downloadingValidator = mockk<CertPathValidator>()
        every { downloadingValidator.revocationChecker } returns offlineValidator.revocationChecker
        every { downloadingValidator.validate(any(), any()) } answers {
            val parameters = secondArg<PKIXParameters>()
            if (parameters.isRevocationEnabled) throw AssertionError("PKIX would download the CRL itself")
            offlineValidator.validate(firstArg(), parameters)
        }
        mockkStatic(CertPathValidator::class)
        every { CertPathValidator.getInstance("PKIX") } returnsMany listOf(downloadingValidator, offlineValidator)
        try {
            val profileValidation = mockk<ProfileValidation>()
            every { profileValidation.validate(any(), any()) } returns true
            val trustStore = ReaderTrustStoreImpl(
                trustedCertificates = listOf(rootCertificate),
                profileValidation = profileValidation,
                errorLogger = { _, message, _ -> logged += message },
                revocationPolicy = policy,
                crlCache = crlCache,
            )
            return trustStore.validateCertificationTrustPath(listOf(leafCertificate, rootCertificate))
        } finally {
            unmockkStatic(CertPathValidator::class)
        }
    }

    // Stands in for a distribution point that serves an HTML page, which the platform reports as a
    // CRLException rather than a network error.
    private fun validateWithUnreadableCrl(
        policy: RevocationPolicy,
        expectedLog: List<String>,
        crlCache: CrlCache? = null,
    ): Boolean {
        val offlineValidator = CertPathValidator.getInstance("PKIX")
        val crlValidator = mockk<CertPathValidator>()
        every { crlValidator.revocationChecker } returns offlineValidator.revocationChecker
        every { crlValidator.validate(any(), any()) } throws
            CertPathValidatorException(CRLException("No certificate found"))
        mockkStatic(CertPathValidator::class)
        every { CertPathValidator.getInstance("PKIX") } returnsMany listOf(crlValidator, offlineValidator)
        try {
            val logged = mutableListOf<String>()
            val profileValidation = mockk<ProfileValidation>()
            every { profileValidation.validate(any(), any()) } returns true
            val trustStore = ReaderTrustStoreImpl(
                trustedCertificates = listOf(rootCertificate),
                profileValidation = profileValidation,
                errorLogger = { _, message, _ -> logged += message },
                revocationPolicy = policy,
                crlCache = crlCache,
            )

            val result = trustStore.validateCertificationTrustPath(listOf(leafCertificate, rootCertificate))

            assertEquals(expectedLog, logged)
            return result
        } finally {
            unmockkStatic(CertPathValidator::class)
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
