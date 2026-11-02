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

package eu.europa.ec.eudi.wallet.registration

import android.util.Log
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.iso18013.transfer.readerauth.RevocationPolicy
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.registration.issuer.DefaultIssuerRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.issuer.IssuerRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.relyingparty.DefaultWrpRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.relyingparty.WrpRegistrationEvaluator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Date

class RegistrationTrustTest {

    private val caKeyPair = ecKeyPair()
    private val ca = certificate(
        subject = X500Name("CN=Test CA"),
        subjectKeyPair = caKeyPair,
        issuer = X500Name("CN=Test CA"),
        issuerKeyPair = caKeyPair,
    )
    private val signer = certificate(
        subject = X500Name("CN=Test Signer"),
        subjectKeyPair = ecKeyPair(),
        issuer = X500Name("CN=Test CA"),
        issuerKeyPair = caKeyPair,
    )

    private val otherCaKeyPair = ecKeyPair()
    private val otherCa = certificate(
        subject = X500Name("CN=Other CA"),
        subjectKeyPair = otherCaKeyPair,
        issuer = X500Name("CN=Other CA"),
        issuerKeyPair = otherCaKeyPair,
    )
    private val otherSigner = certificate(
        subject = X500Name("CN=Other Signer"),
        subjectKeyPair = ecKeyPair(),
        issuer = X500Name("CN=Other CA"),
        issuerKeyPair = otherCaKeyPair,
    )

    private val etsiSource = mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()

    @Test
    fun `the configured certificates take precedence for the registration certificate signer`() = runTest {
        coEvery { etsiSource.invoke(any(), any()) } returns
            CertificationChainValidation.NotTrusted(RuntimeException("not trusted"))

        val certificateTrust = registrationCertificateTrust(
            trustedCertificates = listOf(ca),
            revocationPolicy = RevocationPolicy.NoCheck,
            etsiSource = etsiSource,
        )

        assertTrue(certificateTrust!!.isTrusted(listOf(signer)))
    }

    @Test
    fun `without configured certificates a relying party uses the ETSI registration certificate contexts`() =
        runTest {
            coEvery {
                etsiSource.invoke(any(), VerificationContext.WalletRelyingPartyRegistrationCertificate)
            } returns CertificationChainValidation.Trusted(TrustAnchor(ca, null))
            coEvery {
                etsiSource.invoke(any(), VerificationContext.WalletRelyingPartyRegistrationCertificateStatus)
            } returns CertificationChainValidation.NotTrusted(RuntimeException("not trusted"))

            val trust = RegistrationTrust.forRelyingParty(EudiWalletConfig(), etsiSource)

            assertTrue(trust.certificateTrust!!.isTrusted(listOf(signer)))
            assertFalse(trust.statusTrust!!.isTrusted(listOf(signer)))
        }

    @Test
    fun `the configured status list certificates take precedence`() = runTest {
        coEvery { etsiSource.invoke(any(), any()) } returns
            CertificationChainValidation.NotTrusted(RuntimeException("not trusted"))

        val statusTrust = registrationStatusTrust(
            statusListTrustedCertificates = listOf(ca),
            revocationPolicy = RevocationPolicy.NoCheck,
            etsiSource = etsiSource,
        )

        assertTrue(statusTrust!!.isTrusted(listOf(signer)))
    }

    @Test
    fun `without configured certificates an issuer uses the ETSI registration certificate contexts`() = runTest {
        coEvery {
            etsiSource.invoke(any(), VerificationContext.WalletRelyingPartyRegistrationCertificate)
        } returns CertificationChainValidation.NotTrusted(RuntimeException("not trusted"))
        coEvery {
            etsiSource.invoke(any(), VerificationContext.WalletRelyingPartyRegistrationCertificateStatus)
        } returns CertificationChainValidation.Trusted(TrustAnchor(ca, null))

        val trust = RegistrationTrust.forIssuer(EudiWalletConfig(), etsiSource)

        assertFalse(trust.certificateTrust!!.isTrusted(listOf(signer)))
        assertTrue(trust.statusTrust!!.isTrusted(listOf(signer)))
    }

    @Test
    fun `the configured revocation policy, HardFail by default, applies to both signer chains`() = runTest {
        // the signer names no CRL distribution point, so HardFail cannot confirm its status
        mockkStatic(Log::class)
        try {
            every { Log.d(any(), any(), any()) } returns 0

            val relyingParty = RegistrationTrust.forRelyingParty(
                EudiWalletConfig { configureWrpRegistrationTrust(listOf(ca)) },
                etsiSource = null,
            )
            val issuer = RegistrationTrust.forIssuer(
                EudiWalletConfig { configureIssuerRegistrationTrust(listOf(ca)) },
                etsiSource = null,
            )

            for (trust in listOf(relyingParty, issuer)) {
                assertFalse(trust.certificateTrust!!.isTrusted(listOf(signer)))
                assertFalse(trust.statusTrust!!.isTrusted(listOf(signer)))
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `without etsi and configured certificates a relying party has no registration trust`() {
        val trust = RegistrationTrust.forRelyingParty(EudiWalletConfig(), etsiSource = null)

        assertNull(trust.certificateTrust)
        assertNull(trust.statusTrust)
    }

    @Test
    fun `the certificates configured for relying parties are used for each signer`() = runTest {
        val config = EudiWalletConfig {
            configureWrpRegistrationTrust(listOf(otherCa), listOf(ca), RevocationPolicy.NoCheck)
        }

        val trust = RegistrationTrust.forRelyingParty(config, etsiSource = null)

        assertTrue(trust.certificateTrust!!.isTrusted(listOf(otherSigner)))
        assertTrue(trust.statusTrust!!.isTrusted(listOf(signer)))
    }

    @Test
    fun `the certificates configured for issuers are used for each signer`() = runTest {
        val config = EudiWalletConfig {
            configureIssuerRegistrationTrust(listOf(otherCa), listOf(ca), RevocationPolicy.NoCheck)
        }

        val trust = RegistrationTrust.forIssuer(config, etsiSource = null)

        assertTrue(trust.certificateTrust!!.isTrusted(listOf(otherSigner)))
        assertTrue(trust.statusTrust!!.isTrusted(listOf(signer)))
    }

    @Test
    fun `the built-in evaluators are given the status list trust`() {
        val trust = RegistrationTrust(certificateTrust = { true }, statusTrust = { true })

        val wrpEvaluator = trust.wrpRegistrationEvaluator(custom = null)
        val issuerEvaluator = trust.issuerRegistrationEvaluator(custom = null, isPid = AttestationIdentifierPredicate.None)

        assertSame(trust.statusTrust, (wrpEvaluator as DefaultWrpRegistrationEvaluator).statusTrust)
        assertSame(trust.statusTrust, (issuerEvaluator as DefaultIssuerRegistrationEvaluator).statusTrust)
    }

    @Test
    fun `a configured evaluator replaces the built-in one`() {
        val trust = RegistrationTrust(certificateTrust = { true }, statusTrust = { true })
        val wrpEvaluator = mockk<WrpRegistrationEvaluator>()
        val issuerEvaluator = mockk<IssuerRegistrationEvaluator>()

        assertSame(wrpEvaluator, trust.wrpRegistrationEvaluator(custom = wrpEvaluator))
        assertSame(
            issuerEvaluator,
            trust.issuerRegistrationEvaluator(custom = issuerEvaluator, isPid = AttestationIdentifierPredicate.None),
        )
    }
}

private fun ecKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()

private fun certificate(
    subject: X500Name,
    subjectKeyPair: KeyPair,
    issuer: X500Name,
    issuerKeyPair: KeyPair,
): X509Certificate {
    val now = Instant.now()
    val builder = JcaX509v3CertificateBuilder(
        issuer,
        BigInteger(64, SecureRandom()),
        Date.from(now.minusSeconds(3600)),
        Date.from(now.plusSeconds(86400)),
        subject,
        subjectKeyPair.public,
    )
    val signer = JcaContentSignerBuilder("SHA256withECDSA").build(issuerKeyPair.private)
    return JcaX509CertificateConverter().getCertificate(builder.build(signer))
}
