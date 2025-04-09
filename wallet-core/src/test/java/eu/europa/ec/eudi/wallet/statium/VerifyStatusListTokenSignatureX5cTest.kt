/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.statium

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.util.Base64
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.statium.StatusListTokenFormat
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

class VerifyStatusListTokenSignatureX5cTest {

    private val verifier = VerifyStatusListTokenSignatureX5c()
    private val now = Clock.System.now()

    @Test
    fun `verify successful validation using RSA`() = runBlocking {
        // Generate RSA key pair
        val rsaJWK = RSAKeyGenerator(2048)
            .keyID("123")
            .generate()
        val rsaPublicKey = rsaJWK.toPublicJWK().toRSAKey().toPublicKey()
        val rsaPrivateKey = rsaJWK.toRSAKey().toPrivateKey()

        // Create X509 certificate with the RSA key
        val cert = generateCertificate(rsaPublicKey, rsaPrivateKey)
        val encodedCert = Base64.encode(cert.encoded)

        // Prepare claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("subject")
            .issuer("issuer")
            .expirationTime(Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)))
            .build()

        // Create JWT with x5c header
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .x509CertChain(listOf(encodedCert))
            .build()

        val signedJWT = SignedJWT(header, claimsSet)
        signedJWT.sign(RSASSASigner(rsaPrivateKey))
        val token = signedJWT.serialize()

        // Test verification
        val result = verifier.invoke(token, StatusListTokenFormat.JWT, now)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `verify successful validation using EC`() = runBlocking {
        // Generate EC key pair
        val ecJWK = ECKeyGenerator(Curve.P_256)
            .keyID("123")
            .generate()
        val ecPublicKey = ecJWK.toPublicJWK().toECKey().toPublicKey()
        val ecPrivateKey = ecJWK.toECKey().toPrivateKey() as ECPrivateKey

        // Create X509 certificate with the EC key
        val cert = generateCertificate(ecPublicKey, ecPrivateKey)
        val encodedCert = Base64.encode(cert.encoded)

        // Prepare claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("subject")
            .issuer("issuer")
            .expirationTime(Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC)))
            .build()

        // Create JWT with x5c header
        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .x509CertChain(listOf(encodedCert))
            .build()

        val signedJWT = SignedJWT(header, claimsSet)
        signedJWT.sign(ECDSASigner(ecPrivateKey))
        val token = signedJWT.serialize()

        // Test verification
        val result = verifier.invoke(token, StatusListTokenFormat.JWT, now)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `verify failure with unsupported format`() = runBlocking {
        val result = verifier.invoke("token", StatusListTokenFormat.CWT, now)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(
            "Unsupported format: CWT",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `verify failure with missing x5c header`() = runBlocking {
        // Generate RSA key pair
        val rsaJWK = RSAKeyGenerator(2048)
            .keyID("123")
            .generate()
        val rsaPrivateKey = rsaJWK.toRSAKey().toPrivateKey()

        // Prepare claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("subject")
            .issuer("issuer")
            .build()

        // Create JWT without x5c header
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).build()
        val signedJWT = SignedJWT(header, claimsSet)
        signedJWT.sign(RSASSASigner(rsaPrivateKey))
        val token = signedJWT.serialize()

        // Test verification
        val result = verifier.invoke(token, StatusListTokenFormat.JWT, now)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(
            "Missing x5c in JWT header",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `verify failure with invalid signature`() = runBlocking {
        // Generate two different RSA key pairs - one for signing, one for the certificate
        val signingRsaJWK = RSAKeyGenerator(2048).generate()
        val signingPrivateKey = signingRsaJWK.toRSAKey().toPrivateKey()

        val certRsaJWK = RSAKeyGenerator(2048).generate()
        val certPublicKey = certRsaJWK.toPublicJWK().toRSAKey().toPublicKey()
        val certPrivateKey = certRsaJWK.toRSAKey().toPrivateKey()

        // Create X509 certificate with one key
        val cert = generateCertificate(certPublicKey, certPrivateKey)
        val encodedCert = Base64.encode(cert.encoded)

        // Prepare claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("subject")
            .issuer("issuer")
            .build()

        // Create JWT with x5c header but sign with the different key
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .x509CertChain(listOf(encodedCert))
            .build()

        val signedJWT = SignedJWT(header, claimsSet)
        signedJWT.sign(RSASSASigner(signingPrivateKey)) // Signing with a different key
        val token = signedJWT.serialize()

        // Test verification
        val result = verifier.invoke(token, StatusListTokenFormat.JWT, now)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SignatureVerificationError)
    }

    @Test
    fun `verify failure with malformed JWT`() = runBlocking {
        // Test with malformed JWT
        val result = verifier.invoke("malformed.jwt.token", StatusListTokenFormat.JWT, now)

        assertTrue(result.isFailure)
    }

    private fun generateCertificate(publicKey: PublicKey, privateKey: PrivateKey): X509Certificate {
        val issuerName = X500Name("CN=Test Issuer, O=EUDI")
        val subjectName = X500Name("CN=Test Subject, O=EUDI")

        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365 * 24 * 60 * 60 * 1000L) // Valid for 1 year

        val certBuilder = JcaX509v3CertificateBuilder(
            issuerName,
            serial,
            notBefore,
            notAfter,
            subjectName,
            publicKey
        )

        val sigAlg = if (privateKey.algorithm == "RSA") "SHA256withRSA" else "SHA256withECDSA"
        val contentSigner = JcaContentSignerBuilder(sigAlg).build(privateKey)

        return JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner))
    }
}
