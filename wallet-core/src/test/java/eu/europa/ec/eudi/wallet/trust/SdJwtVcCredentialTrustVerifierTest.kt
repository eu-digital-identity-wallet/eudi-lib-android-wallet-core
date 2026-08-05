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
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.util.Base64 as NimbusBase64
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForAttestation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.util.Date
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class SdJwtVcCredentialTrustVerifierTest {

    private val attestationIdentifier = AttestationIdentifier.SDJwtVc("VerifiablePortableDocumentA1")
    private val isChainTrusted =
        mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()
    private val verifier = SdJwtVcCredentialTrustVerifier(isChainTrusted)

    @Test
    fun returnsTrustedForValidSdJwt() = runTest {
        val keyPair = generateEcKeyPair()
        val cert = createSelfSignedCert(keyPair, "CN=Test Issuer")
        val sdJwt = buildSdJwt(keyPair, cert)

        val trustAnchor = mockk<TrustAnchor>()
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { isChainTrusted.issuance(match { certs ->
            certs.size == 1 && certs[0].encoded.contentEquals(cert.encoded)
        }, attestationIdentifier) } returns trusted

        val result = verifier.verify(sdJwt, attestationIdentifier)

        assertNotNull(result)
        assertIs<CertificationChainValidation.Trusted<TrustAnchor>>(result)
        assertEquals(trustAnchor, result.trustAnchor)
    }

    @Test
    fun returnsNotTrustedForUntrustedChain() = runTest {
        val keyPair = generateEcKeyPair()
        val cert = createSelfSignedCert(keyPair, "CN=Untrusted Issuer")
        val sdJwt = buildSdJwt(keyPair, cert)

        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { isChainTrusted.issuance(any(), eq(attestationIdentifier)) } returns notTrusted

        val result = verifier.verify(sdJwt, attestationIdentifier)

        assertNotNull(result)
        assertIs<CertificationChainValidation.NotTrusted>(result)
        assertEquals(cause, result.cause)
    }

    @Test
    fun returnsNullForMissingX5cHeader() = runTest {
        val keyPair = generateEcKeyPair()
        // Build a JWT without x5c header
        val header = JWSHeader.Builder(JWSAlgorithm.ES256).build()
        val claims = JWTClaimsSet.Builder()
            .issuer("https://example.com")
            .claim("vct", "VerifiablePortableDocumentA1")
            .build()
        val signedJwt = SignedJWT(header, claims)
        signedJwt.sign(ECDSASigner(keyPair.private as ECPrivateKey))
        val sdJwt = "${signedJwt.serialize()}~"

        val result = verifier.verify(sdJwt, attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun returnsNullForMalformedJwt() = runTest {
        val result = verifier.verify("not-a-valid-jwt~", attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun handlesSdJwtWithDisclosures() = runTest {
        val keyPair = generateEcKeyPair()
        val cert = createSelfSignedCert(keyPair, "CN=Test Issuer")

        // Build a SD-JWT with a disclosure segment
        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .x509CertChain(listOf(NimbusBase64.encode(cert.encoded)))
            .build()
        val claims = JWTClaimsSet.Builder()
            .issuer("https://example.com")
            .claim("vct", "VerifiablePortableDocumentA1")
            .claim("_sd_alg", "sha-256")
            .build()
        val signedJwt = SignedJWT(header, claims)
        signedJwt.sign(ECDSASigner(keyPair.private as ECPrivateKey))
        // Append a fake disclosure and trailing tilde
        val sdJwt = "${signedJwt.serialize()}~WyJzYWx0IiwiZ2l2ZW5fbmFtZSIsIkpvaG4iXQ~"

        val trustAnchor = mockk<TrustAnchor>()
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { isChainTrusted.issuance(match { certs ->
            certs.size == 1 && certs[0].encoded.contentEquals(cert.encoded)
        }, attestationIdentifier) } returns trusted

        val result = verifier.verify(sdJwt, attestationIdentifier)

        assertNotNull(result)
        assertIs<CertificationChainValidation.Trusted<TrustAnchor>>(result)
    }

    // -- helpers --

    private fun generateEcKeyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private fun createSelfSignedCert(
        keyPair: java.security.KeyPair,
        cn: String,
    ): X509Certificate {
        val issuer = org.bouncycastle.asn1.x500.X500Name(cn)
        val notBefore = Date(System.currentTimeMillis() - 86400000L)
        val notAfter = Date(System.currentTimeMillis() + 30 * 86400000L)
        val builder = JcaX509v3CertificateBuilder(
            issuer, BigInteger.ONE, notBefore, notAfter, issuer, keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256WithECDSA").build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }

    private fun buildSdJwt(
        keyPair: java.security.KeyPair,
        cert: X509Certificate,
    ): String {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .x509CertChain(listOf(NimbusBase64.encode(cert.encoded)))
            .build()
        val claims = JWTClaimsSet.Builder()
            .issuer("https://example.com")
            .claim("vct", "VerifiablePortableDocumentA1")
            .build()
        val signedJwt = SignedJWT(header, claims)
        signedJwt.sign(ECDSASigner(keyPair.private as ECPrivateKey))
        return "${signedJwt.serialize()}~"
    }
}
