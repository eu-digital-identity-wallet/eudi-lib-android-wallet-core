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
import eu.europa.ec.eudi.openid4vci.BindingKey
import eu.europa.ec.eudi.wallet.issue.openid4vci.DPoPSigner
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DPopSignerTest {

    private lateinit var dPoPSigner: DPoPSigner

    @Before
    fun setUp() {
        dPoPSigner = DPoPSigner()
    }

    @After
    fun tearDown() {
        // Clean up any resources here
    }

    @Test
    fun testGetAlgorithm() {
        val algorithm = dPoPSigner.getAlgorithm()
        assertEquals(JWSAlgorithm.ES256, algorithm)
    }

    @Test
    fun testGetBindingKey() {
        val bindingKey = dPoPSigner.getBindingKey()
        assertThat(bindingKey, instanceOf(BindingKey.Jwk::class.java))
    }

    @Test
    fun testSupportedJWSAlgorithms() {
        val algorithms = dPoPSigner.supportedJWSAlgorithms()
        assertThat(algorithms, hasSize(1))
        assertThat(algorithms, contains(JWSAlgorithm.ES256))
    }

    @Test(expected = JOSEException::class)
    fun testSignThrowsWhenUnsupportedAlgorithmInHeader() {
        val header = JWSHeader(JWSAlgorithm.ES384)
        val signingInput = "test".toByteArray()

        dPoPSigner.sign(header, signingInput)
    }

    @Test
    fun testSignReturnsValidSignature() {
        val header = JWSHeader(JWSAlgorithm.ES256)
        val signingInput = "test".toByteArray()
        val signature = dPoPSigner.sign(header, signingInput)
        val jwk = (dPoPSigner.getBindingKey() as BindingKey.Jwk).jwk
        val verifier = ECDSAVerifier(jwk.toECKey())
        val result = verifier.verify(header, signingInput, signature)
        assertTrue(result)
    }
}