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

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertTrue
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
import kotlin.time.Clock
import com.nimbusds.jose.util.Base64 as NimbusBase64

class RegistrationStatusTokenVerifierTest {

    private val signerKeyPair = ecKeyPair()
    private val signerCertificate = selfSignedCertificate(signerKeyPair, X500Name("CN=Status List Provider"))
    private val statusListToken = statusListToken(signerKeyPair, signerCertificate)

    @Test
    fun `a status list token whose signer chain is trusted is accepted`() = runTest {
        val verifier = RegistrationStatusTokenVerifier(statusTrust = { true })

        val result = verifier.invoke(statusListToken, Clock.System.now())

        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
    }

    @Test
    fun `a status list token whose signer chain is not trusted is rejected`() = runTest {
        val verifier = RegistrationStatusTokenVerifier(statusTrust = { false })

        val result = verifier.invoke(statusListToken, Clock.System.now())

        assertTrue(result.isFailure)
    }
}

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

/** A status list token signed with [signerKeyPair], carrying [signerCertificate] in its `x5c` header. */
private fun statusListToken(signerKeyPair: KeyPair, signerCertificate: X509Certificate): String {
    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(JOSEObjectType("statuslist+jwt"))
        .x509CertChain(listOf(NimbusBase64.encode(signerCertificate.encoded)))
        .build()
    val claims = JWTClaimsSet.Builder()
        .subject("https://registrar.example/status/1")
        .claim("status_list", mapOf("bits" to 1, "lst" to "eNrbuRgAAhcBXQ"))
        .build()
    return SignedJWT(header, claims)
        .apply { sign(ECDSASigner(signerKeyPair.private as ECPrivateKey)) }
        .serialize()
}
