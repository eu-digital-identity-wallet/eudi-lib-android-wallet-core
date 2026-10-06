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
package eu.europa.ec.eudi.wallet.trust

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.util.Base64
import eu.europa.ec.eudi.etsi119602.consultation.VerifyJwtSignature
import eu.europa.ec.eudi.etsi1196x2.consultation.NonEmptyList
import eu.europa.ec.eudi.etsi1196x2.consultation.ValidateCertificateChainUsingDirectTrustJvm
import eu.europa.ec.eudi.etsi1196x2.consultation.ValidateCertificateChainUsingPKIXJvm
import eu.europa.ec.eudi.etsi1196x2.consultation.or
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertIs

class LoteJwtVerifierTest {

    private val caKeyPair = ecKeyPair()
    private val caCertificate = certificate("CN=LoTE Signer CA", caKeyPair, isCa = true)

    private val signerKeyPair = ecKeyPair()
    private val signerCertificate = certificate("CN=LoTE Signer", signerKeyPair, issuer = caCertificate, issuerKeyPair = caKeyPair)

    @Test
    fun `verifies a LoTE whose signer is issued by a trust anchor`() = runTest {
        val outcome = verifier(caCertificate)(lote(signerKeyPair, signerCertificate))

        assertIs<VerifyJwtSignature.Outcome.Verified>(outcome)
    }

    @Test
    fun `verifies a LoTE whose signer is a trust anchor`() = runTest {
        val outcome = verifier(signerCertificate)(lote(signerKeyPair, signerCertificate))

        assertIs<VerifyJwtSignature.Outcome.Verified>(outcome)
    }

    @Test
    fun `does not verify a LoTE signed with a self-signed certificate`() = runTest {
        val attackerKeyPair = ecKeyPair()
        val attackerCertificate = certificate("CN=LoTE Signer", attackerKeyPair)

        val outcome = verifier(caCertificate)(lote(attackerKeyPair, attackerCertificate))

        assertIs<VerifyJwtSignature.Outcome.NotVerified>(outcome)
    }

    @Test
    fun `does not verify a LoTE that carries a trusted certificate but is signed with another key`() = runTest {
        val outcome = verifier(caCertificate)(lote(ecKeyPair(), signerCertificate))

        assertIs<VerifyJwtSignature.Outcome.NotVerified>(outcome)
    }

    @Test
    fun `does not verify a LoTE whose signing certificate has expired`() = runTest {
        val expiredKeyPair = ecKeyPair()
        val expiredCertificate = certificate(
            "CN=LoTE Signer",
            expiredKeyPair,
            notBefore = Instant.now() - Duration.ofDays(30),
            notAfter = Instant.now() - Duration.ofDays(1),
        )

        val outcome = verifier(expiredCertificate)(lote(expiredKeyPair, expiredCertificate))

        assertIs<VerifyJwtSignature.Outcome.NotVerified>(outcome)
    }

    @Test
    fun `does not verify a LoTE with a critical header parameter`() = runTest {
        val outcome = verifier(caCertificate)(lote(signerKeyPair, signerCertificate, crit = setOf("sigT")))

        assertIs<VerifyJwtSignature.Outcome.NotVerified>(outcome)
    }

    private fun verifier(vararg anchors: X509Certificate) = LoteJwtVerifier(
        signerTrustAnchors = NonEmptyList(anchors.map { TrustAnchor(it, null) }),
        validateSigner = ValidateCertificateChainUsingDirectTrustJvm or
            ValidateCertificateChainUsingPKIXJvm(customization = { isRevocationEnabled = false }),
    )

    private fun lote(signerKeyPair: KeyPair, signerCertificate: X509Certificate, crit: Set<String> = emptySet()): String {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .x509CertChain(listOf(Base64.encode(signerCertificate.encoded)))
            .apply { if (crit.isNotEmpty()) criticalParams(crit) }
            .apply { crit.forEach { customParam(it, "2026-09-11T16:06:07Z") } }
            .build()
        return JWSObject(header, Payload("""{"LoTE":{}}"""))
            .apply { sign(ECDSASigner(signerKeyPair.private as ECPrivateKey)) }
            .serialize()
    }

    private fun ecKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()

    private fun certificate(
        subject: String,
        keyPair: KeyPair,
        issuer: X509Certificate? = null,
        issuerKeyPair: KeyPair = keyPair,
        isCa: Boolean = false,
        notBefore: Instant = Instant.now() - Duration.ofDays(1),
        notAfter: Instant = Instant.now() + Duration.ofDays(30),
    ): X509Certificate {
        val builder = JcaX509v3CertificateBuilder(
            issuer?.let { X500Name(it.subjectX500Principal.name) } ?: X500Name(subject),
            BigInteger(64, SecureRandom()),
            Date.from(notBefore),
            Date.from(notAfter),
            X500Name(subject),
            keyPair.public,
        )
            .addExtension(Extension.basicConstraints, true, BasicConstraints(isCa))
            .addExtension(Extension.keyUsage, true, KeyUsage(if (isCa) KeyUsage.keyCertSign else KeyUsage.digitalSignature))
        val contentSigner = JcaContentSignerBuilder("SHA256withECDSA").build(issuerKeyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(contentSigner))
    }
}
