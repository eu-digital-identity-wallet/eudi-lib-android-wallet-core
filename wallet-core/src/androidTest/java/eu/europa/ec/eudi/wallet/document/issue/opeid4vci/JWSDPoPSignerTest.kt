/*
 *  Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.document.issue.opeid4vci

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSAVerifier
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.JWSDPoPSigner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JWSDPoPSignerTest {

    @Test
    fun factoryInvokeCreatesDPoPSigner() {
        val signer = JWSDPoPSigner().getOrNull()
        Assert.assertTrue(signer is PopSigner.Jwt)
    }

    @Test
    fun signerSupportedJWSAlgorithmsIsES256() {
        val signer = JWSDPoPSigner().getOrThrow()
        Assert.assertEquals(JWSAlgorithm.ES256, signer.algorithm)
        Assert.assertEquals(1, signer.jwsSigner.supportedJWSAlgorithms().size)
        Assert.assertEquals(JWSAlgorithm.ES256, signer.jwsSigner.supportedJWSAlgorithms().first())
    }

    @Test
    fun bindingKeyIsJwtBindingKey_Jwk() {
        val signer = JWSDPoPSigner().getOrThrow()
        Assert.assertTrue(signer.bindingKey is JwtBindingKey.Jwk)
    }

    @Test(expected = JOSEException::class)
    fun signThrowsWhenUnsupportedAlgorithmInHeader() {
        val signer = JWSDPoPSigner().getOrThrow()
        val header = JWSHeader(JWSAlgorithm.ES384)
        val signingInput = "test".toByteArray()
        signer.jwsSigner.sign(header, signingInput)
    }

    @Test
    fun signReturnsValidSignature() {
        val signer = JWSDPoPSigner().getOrThrow()
        val header = JWSHeader(JWSAlgorithm.ES256)
        val signingInput = "test".toByteArray()
        val signature = signer.jwsSigner.sign(header, signingInput)
        val jwk = (signer.bindingKey as JwtBindingKey.Jwk).jwk
        val verifier = ECDSAVerifier(jwk.toECKey())
        val result = verifier.verify(header, signingInput, signature)
        Assert.assertTrue(result)
    }

    @Test
    fun instantiatingDPoPSignerUsesDifferentKeyPair() {
        val firstSigner = JWSDPoPSigner().getOrThrow()
        val firstJwk = (firstSigner.bindingKey as JwtBindingKey.Jwk).jwk
        val secondSigner = JWSDPoPSigner().getOrThrow()
        val secondJwk = (secondSigner.bindingKey as JwtBindingKey.Jwk).jwk
        Assert.assertNotEquals(firstJwk, secondJwk)
    }
}