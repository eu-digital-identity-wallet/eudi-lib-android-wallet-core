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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForAttestation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseSign1
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.javaPrivateKey
import org.multipaz.crypto.javaPublicKey
import org.multipaz.crypto.javaX509Certificates
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class MsoMdocCredentialTrustVerifierTest {

    private val attestationIdentifier = AttestationIdentifier.MDoc("org.iso.18013.5.1.mDL")
    private val isChainTrusted =
        mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()
    private val verifier = MsoMdocCredentialTrustVerifier(isChainTrusted)

    @Test
    fun returnsNullForMalformedBase64() = runTest {
        val result = verifier.verify("not-valid-base64!!!", attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun returnsNullForMalformedCbor() = runTest {
        val badCbor = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(byteArrayOf(0xFF.toByte(), 0x01))
        val result = verifier.verify(badCbor, attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun returnsNullForEmptyCredentialValue() = runTest {
        val result = verifier.verify("", attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun returnsNullWhenX5chainMissing() = runTest {
        val coseSign1 = CoseSign1(
            protectedHeaders = mapOf(
                Cose.COSE_LABEL_ALG.toCoseLabel to
                    Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
            ),
            unprotectedHeaders = emptyMap(),
            signature = ByteArray(64),
            payload = ByteArray(10),
        )

        val credentialValue = encodeAsStaticAuthData(coseSign1)
        val result = verifier.verify(credentialValue, attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun returnsTrustedForValidCredential() = runTest {
        val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val dsCert = createSelfSignedCert(dsKey, "CN=Test DS")
        val x5chain = X509CertChain(listOf(dsCert))
        val javaCerts = x5chain.javaX509Certificates

        val coseSign1 = signCoseSign1(dsKey, x5chain)
        val credentialValue = encodeAsStaticAuthData(coseSign1)

        val trustAnchor = mockk<TrustAnchor>()
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { isChainTrusted.issuance(javaCerts, attestationIdentifier) } returns trusted

        val result = verifier.verify(credentialValue, attestationIdentifier)

        assertNotNull(result)
        assertIs<CertificationChainValidation.Trusted<TrustAnchor>>(result)
        assertEquals(trustAnchor, result.trustAnchor)
    }

    @Test
    fun returnsNotTrustedForUntrustedChain() = runTest {
        val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val dsCert = createSelfSignedCert(dsKey, "CN=Untrusted DS")
        val x5chain = X509CertChain(listOf(dsCert))
        val javaCerts = x5chain.javaX509Certificates

        val coseSign1 = signCoseSign1(dsKey, x5chain)
        val credentialValue = encodeAsStaticAuthData(coseSign1)

        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { isChainTrusted.issuance(javaCerts, attestationIdentifier) } returns notTrusted

        val result = verifier.verify(credentialValue, attestationIdentifier)

        assertNotNull(result)
        assertIs<CertificationChainValidation.NotTrusted>(result)
        assertEquals(cause, result.cause)
    }

    @Test
    fun returnsNullWhenIssuanceReturnsNull() = runTest {
        val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val dsCert = createSelfSignedCert(dsKey, "CN=Test DS")
        val x5chain = X509CertChain(listOf(dsCert))
        val javaCerts = x5chain.javaX509Certificates

        val coseSign1 = signCoseSign1(dsKey, x5chain)
        val credentialValue = encodeAsStaticAuthData(coseSign1)

        coEvery { isChainTrusted.issuance(javaCerts, attestationIdentifier) } returns null

        val result = verifier.verify(credentialValue, attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun returnsNullWhenSignatureInvalid() = runTest {
        val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val dsCert = createSelfSignedCert(dsKey, "CN=Test DS")
        val x5chain = X509CertChain(listOf(dsCert))
        val javaCerts = x5chain.javaX509Certificates

        // Build a COSE_Sign1 with a bogus signature
        val coseSign1 = CoseSign1(
            protectedHeaders = mapOf(
                Cose.COSE_LABEL_ALG.toCoseLabel to
                    Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
            ),
            unprotectedHeaders = mapOf(
                Cose.COSE_LABEL_X5CHAIN.toCoseLabel to x5chain.toDataItem()
            ),
            signature = ByteArray(64),
            payload = ByteArray(10),
        )

        val credentialValue = encodeAsStaticAuthData(coseSign1)

        val trustAnchor = mockk<TrustAnchor>()
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { isChainTrusted.issuance(javaCerts, attestationIdentifier) } returns trusted

        val result = verifier.verify(credentialValue, attestationIdentifier)
        assertNull(result)
    }

    @Test
    fun handlesMultipleCertificatesInX5chain() = runTest {
        val iacaKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)

        val iacaCert = createSelfSignedCert(iacaKey, "CN=Test IACA")
        val dsCert = createCert(dsKey, iacaKey, "CN=Test DS", "CN=Test IACA")

        val x5chain = X509CertChain(listOf(dsCert, iacaCert))
        val javaCerts = x5chain.javaX509Certificates

        val coseSign1 = signCoseSign1(dsKey, x5chain)
        val credentialValue = encodeAsStaticAuthData(coseSign1)

        val trustAnchor = mockk<TrustAnchor>()
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { isChainTrusted.issuance(javaCerts, attestationIdentifier) } returns trusted

        val result = verifier.verify(credentialValue, attestationIdentifier)

        assertNotNull(result)
        assertIs<CertificationChainValidation.Trusted<TrustAnchor>>(result)
        assertTrue(javaCerts.size == 2)
    }

    // -- helpers --

    @Suppress("DEPRECATION")
    private suspend fun signCoseSign1(
        signingKey: EcPrivateKey,
        x5chain: X509CertChain,
    ): CoseSign1 {
        val payload = ByteArray(10) { it.toByte() }
        return Cose.coseSign1Sign(
            key = signingKey,
            dataToSign = payload,
            includeDataInPayload = true,
            signatureAlgorithm = Algorithm.ES256,
            protectedHeaders = mapOf(
                Cose.COSE_LABEL_ALG.toCoseLabel to
                    Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
            ),
            unprotectedHeaders = mapOf(
                Cose.COSE_LABEL_X5CHAIN.toCoseLabel to x5chain.toDataItem()
            ),
        )
    }

    private fun encodeAsStaticAuthData(coseSign1: CoseSign1): String {
        val staticAuthData = buildCborMap {
            put("issuerAuth", coseSign1.toDataItem())
        }
        val encoded = Cbor.encode(staticAuthData)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded)
    }

    private fun createSelfSignedCert(
        key: EcPrivateKey,
        cn: String,
    ): X509Cert {
        val javaPublicKey = key.publicKey.javaPublicKey
        val javaPrivateKey = key.javaPrivateKey
        val issuer = org.bouncycastle.asn1.x500.X500Name(cn)
        val notBefore = Date(System.currentTimeMillis() - 86400000L)
        val notAfter = Date(System.currentTimeMillis() + 30 * 86400000L)
        val builder = JcaX509v3CertificateBuilder(
            issuer, BigInteger.ONE, notBefore, notAfter, issuer, javaPublicKey,
        )
        val signer = JcaContentSignerBuilder("SHA256WithECDSA").build(javaPrivateKey)
        val javaCert = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        return X509Cert(ByteString(javaCert.encoded))
    }

    private fun createCert(
        subjectKey: EcPrivateKey,
        signingKey: EcPrivateKey,
        subjectCn: String,
        issuerCn: String,
    ): X509Cert {
        val javaPublicKey = subjectKey.publicKey.javaPublicKey
        val javaSigningPrivateKey = signingKey.javaPrivateKey
        val issuer = org.bouncycastle.asn1.x500.X500Name(issuerCn)
        val subject = org.bouncycastle.asn1.x500.X500Name(subjectCn)
        val notBefore = Date(System.currentTimeMillis() - 86400000L)
        val notAfter = Date(System.currentTimeMillis() + 30 * 86400000L)
        val builder = JcaX509v3CertificateBuilder(
            issuer, BigInteger.TWO, notBefore, notAfter, subject, javaPublicKey,
        )
        val signer = JcaContentSignerBuilder("SHA256WithECDSA").build(javaSigningPrivateKey)
        val javaCert = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        return X509Cert(ByteString(javaCert.encoded))
    }
}
