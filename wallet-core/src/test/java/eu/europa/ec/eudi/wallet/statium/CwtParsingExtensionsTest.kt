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
package eu.europa.ec.eudi.wallet.statium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseSign1
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CwtParsingExtensionsTest {

    @Test
    fun decodeCoseSign1UnwrapsTag18() {
        val coseSign1 = buildMinimalCoseSign1()
        val encoded = Cbor.encode(coseSign1.toDataItem())
        // Wrap in CBOR tag 18 (COSE_Sign1)
        val tagged = Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem())
        val taggedEncoded = Cbor.encode(tagged)

        val result = decodeCoseSign1(taggedEncoded)
        assertNotNull(result)
        assertEquals(coseSign1.payload?.size, result.payload?.size)
    }

    @Test
    fun decodeCoseSign1AcceptsUntagged() {
        val coseSign1 = buildMinimalCoseSign1()
        val encoded = Cbor.encode(coseSign1.toDataItem())

        val result = decodeCoseSign1(encoded)
        assertNotNull(result)
        assertEquals(coseSign1.payload?.size, result.payload?.size)
    }

    @Test
    fun extractX5chainFromProtectedHeaders() {
        val x5chain = buildSelfSignedX5chain()
        val coseSign1 = CoseSign1(
            protectedHeaders = mapOf(
                Cose.COSE_LABEL_ALG.toCoseLabel to
                    Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem(),
                Cose.COSE_LABEL_X5CHAIN.toCoseLabel to x5chain.toDataItem(),
            ),
            unprotectedHeaders = emptyMap(),
            signature = ByteArray(64),
            payload = ByteArray(10),
        )

        val result = coseSign1.extractX5chain()
        assertEquals(1, result.certificates.size)
    }

    @Test
    fun extractX5chainFromUnprotectedHeaders() {
        val x5chain = buildSelfSignedX5chain()
        val coseSign1 = CoseSign1(
            protectedHeaders = mapOf(
                Cose.COSE_LABEL_ALG.toCoseLabel to
                    Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem(),
            ),
            unprotectedHeaders = mapOf(
                Cose.COSE_LABEL_X5CHAIN.toCoseLabel to x5chain.toDataItem(),
            ),
            signature = ByteArray(64),
            payload = ByteArray(10),
        )

        val result = coseSign1.extractX5chain()
        assertEquals(1, result.certificates.size)
    }

    @Test
    fun extractX5chainPrefersProtected() {
        val protectedChain = buildSelfSignedX5chain()
        val unprotectedChain = buildSelfSignedX5chain()
        val coseSign1 = CoseSign1(
            protectedHeaders = mapOf(
                Cose.COSE_LABEL_ALG.toCoseLabel to
                    Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem(),
                Cose.COSE_LABEL_X5CHAIN.toCoseLabel to protectedChain.toDataItem(),
            ),
            unprotectedHeaders = mapOf(
                Cose.COSE_LABEL_X5CHAIN.toCoseLabel to unprotectedChain.toDataItem(),
            ),
            signature = ByteArray(64),
            payload = ByteArray(10),
        )

        val result = coseSign1.extractX5chain()
        // Should return the protected chain's cert
        assertEquals(
            protectedChain.certificates.first().encodedCertificate,
            result.certificates.first().encodedCertificate,
        )
    }

    @Test(expected = IllegalStateException::class)
    fun extractX5chainThrowsWhenMissing() {
        val coseSign1 = CoseSign1(
            protectedHeaders = mapOf(
                Cose.COSE_LABEL_ALG.toCoseLabel to
                    Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem(),
            ),
            unprotectedHeaders = emptyMap(),
            signature = ByteArray(64),
            payload = ByteArray(10),
        )

        coseSign1.extractX5chain()
    }

    // -- helpers --

    private fun buildMinimalCoseSign1(): CoseSign1 = CoseSign1(
        protectedHeaders = mapOf(
            Cose.COSE_LABEL_ALG.toCoseLabel to
                Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem(),
        ),
        unprotectedHeaders = emptyMap(),
        signature = ByteArray(64),
        payload = ByteArray(10),
    )

    private fun buildSelfSignedX5chain(): X509CertChain {
        val key = Crypto.createEcPrivateKey(EcCurve.P256)
        val cert = X509Cert.Builder(
            publicKey = key.publicKey,
            signingKey = key,
            signatureAlgorithm = Algorithm.ES256,
            serialNumber = "1",
            subject = "CN=Test",
            issuer = "CN=Test",
            validFrom = kotlinx.datetime.Clock.System.now(),
            validUntil = kotlinx.datetime.Clock.System.now()
                .plus(kotlinx.datetime.DateTimePeriod(days = 30), kotlinx.datetime.TimeZone.UTC),
        ).build()
        return X509CertChain(listOf(cert))
    }
}
