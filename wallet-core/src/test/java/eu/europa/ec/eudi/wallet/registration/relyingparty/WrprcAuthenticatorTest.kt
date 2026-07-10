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
package eu.europa.ec.eudi.wallet.registration.relyingparty

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Date
import kotlin.test.assertIs
import com.nimbusds.jose.util.Base64 as NimbusBase64

class WrprcAuthenticatorTest {

    private val certificateTrust = mockk<ReaderTrustStore>()

    private val providerKeyPair = ecKeyPair()
    private val providerCertificate = selfSignedCertificate(providerKeyPair, X500Name("CN=WRPRC Provider"))

    @Test
    fun `a well-formed trusted certificate is authenticated`() = runTest {
        every { certificateTrust.validateCertificationTrustPath(any()) } returns true
        val serialized = wrprcJwt(providerKeyPair, providerCertificate, payload(sub = "ORG-123"))

        val result = DefaultWrprcAuthenticator(certificateTrust).authenticate(serialized)

        val authentic = assertIs<WrprcAuthentication.Authentic>(result)
        assertEquals("Nordic Bank", authentic.registration.name)
        assertEquals("ORG-123", authentic.registration.identifiers.single().value)
    }

    @Test
    fun `an unparseable certificate is rejected as malformed`() = runTest {
        val result = DefaultWrprcAuthenticator(certificateTrust)
            .authenticate("this is not a token".toByteArray())

        assertEquals(RegistrationFailureReason.MALFORMED, result.failureReason())
    }

    @Test
    fun `a certificate without a signer chain is rejected`() = runTest {
        val serialized = wrprcJwtWithoutChain(providerKeyPair, payload(sub = "ORG-123"))

        val result = DefaultWrprcAuthenticator(certificateTrust).authenticate(serialized)

        assertEquals(RegistrationFailureReason.SIGNATURE_INVALID, result.failureReason())
    }

    @Test
    fun `a certificate whose signature does not match its signer chain is rejected`() = runTest {
        val unrelatedCertificate = selfSignedCertificate(ecKeyPair(), X500Name("CN=Unrelated"))
        // Signed with the provider key, but carrying an unrelated certificate in its chain.
        val serialized = wrprcJwt(providerKeyPair, unrelatedCertificate, payload(sub = "ORG-123"))

        val result = DefaultWrprcAuthenticator(certificateTrust).authenticate(serialized)

        assertEquals(RegistrationFailureReason.SIGNATURE_INVALID, result.failureReason())
    }

    @Test
    fun `a certificate from an untrusted provider is rejected`() = runTest {
        every { certificateTrust.validateCertificationTrustPath(any()) } returns false
        val serialized = wrprcJwt(providerKeyPair, providerCertificate, payload(sub = "ORG-123"))

        val result = DefaultWrprcAuthenticator(certificateTrust).authenticate(serialized)

        assertEquals(RegistrationFailureReason.UNTRUSTED_PROVIDER, result.failureReason())
    }

    @Test
    fun `a failing trust evaluation is treated as untrusted`() = runTest {
        every { certificateTrust.validateCertificationTrustPath(any()) } throws
            RuntimeException("trust store unavailable")
        val serialized = wrprcJwt(providerKeyPair, providerCertificate, payload(sub = "ORG-123"))

        val result = DefaultWrprcAuthenticator(certificateTrust).authenticate(serialized)

        assertEquals(RegistrationFailureReason.UNTRUSTED_PROVIDER, result.failureReason())
    }

    @Test
    fun `a certificate with an unexpected typ header is rejected as malformed`() = runTest {
        every { certificateTrust.validateCertificationTrustPath(any()) } returns true
        // typ must be rc-wrp+jwt
        val serialized = wrprcJwt(providerKeyPair, providerCertificate, payload(sub = "ORG-123"), type = "jwt")

        val result = DefaultWrprcAuthenticator(certificateTrust).authenticate(serialized)

        assertEquals(RegistrationFailureReason.MALFORMED, result.failureReason())
    }
}

private fun WrprcAuthentication.failureReason(): RegistrationFailureReason =
    assertIs<WrprcAuthentication.Invalid>(this).reason

private fun payload(sub: String, name: String = "Nordic Bank"): String =
    """{"sub":"$sub","name":"$name"}"""

private fun ecKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()

private fun selfSignedCertificate(keyPair: KeyPair, subject: X500Name): X509Certificate {
    val now = Instant.now()
    val builder = JcaX509v3CertificateBuilder(
        subject,
        BigInteger(64, SecureRandom()),
        Date.from(now.minusSeconds(3600)),
        Date.from(now.plusSeconds(86400)),
        subject,
        keyPair.public,
    )
    val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
    return JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

private const val REG_CERT_TYPE_JWT = "rc-wrp+jwt"

private fun wrprcJwt(
    signerKeyPair: KeyPair,
    chainCertificate: X509Certificate,
    payloadJson: String,
    type: String? = REG_CERT_TYPE_JWT,
): ByteArray {
    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .apply { type?.let { type(JOSEObjectType(it)) } }
        .x509CertChain(listOf(NimbusBase64.encode(chainCertificate.encoded)))
        .build()
    return signedCompact(header, payloadJson, signerKeyPair)
}

private fun wrprcJwtWithoutChain(signerKeyPair: KeyPair, payloadJson: String): ByteArray =
    signedCompact(
        JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType(REG_CERT_TYPE_JWT)).build(),
        payloadJson,
        signerKeyPair,
    )

private fun signedCompact(header: JWSHeader, payloadJson: String, signerKeyPair: KeyPair): ByteArray {
    val jwt = SignedJWT(header, JWTClaimsSet.parse(payloadJson))
    jwt.sign(ECDSASigner(signerKeyPair.private as ECPrivateKey))
    return jwt.serialize().toByteArray(Charsets.US_ASCII)
}
