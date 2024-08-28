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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.app.KeyguardManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.openid4vci.CNonce
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.wallet.document.CreateDocumentResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.util.*
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofType.ProofAlgorithm as SupportedProofAlgorithm

@RunWith(AndroidJUnit4::class)
class JWSProofSignerTest {
    companion object {
        private lateinit var context: Context

        @JvmStatic
        @BeforeClass
        @Throws(IOException::class)
        fun setUp() {
            context = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    @Test
    fun test_userAuthRequired_is_set_to_Yes_when_needed() {
        assumeTrue(
            "Device is not secure. So cannot run this test",
            context.getSystemService(KeyguardManager::class.java).isDeviceSecure
        )
        val documentManager = DocumentManager.Builder(context)
            .enableUserAuth(true)
            .build()


        val createDocumentResult = documentManager.createDocument("eu.europa.ec.eudi.pid.1", false)
        assertTrue(createDocumentResult is CreateDocumentResult.Success)

        val unsignedDocument =
            (createDocumentResult as CreateDocumentResult.Success).unsignedDocument

        val proofSigner = JWSProofSigner(unsignedDocument, SupportedProofAlgorithm.Jws.ES256)
        val algorithm = proofSigner.popSigner.algorithm

        assertTrue(proofSigner.popSigner.bindingKey is JwtBindingKey.Jwk)
        val publicKey = proofSigner.popSigner.bindingKey as JwtBindingKey.Jwk

        val header = JWSHeader.Builder(algorithm).apply {
            type(JOSEObjectType("openid4vci-proof+jwt"))
            jwk(publicKey.jwk)
        }.build()

        val claimsSet = JWTClaimsSet.Builder().apply {
            audience("https://example.openid4vci.com")
            claim("nonce", CNonce("1234567890").value)
            issueTime(Date.from(Instant.now()))
        }.build()

        try {
            proofSigner.sign(header, claimsSet.toPayload().toBytes())
            Assert.fail("Expected UserAuthRequiredException")
        } catch (e: IllegalStateException) {
            assertTrue(proofSigner.userAuthStatus is ProofSigner.UserAuthStatus.Required)
        } finally {
            documentManager.deleteDocumentById(unsignedDocument.id)
        }
    }

    @Test
    fun test_sign_method_creates_a_valid_signature() {
        val documentManager = DocumentManager.Builder(context)
            .enableUserAuth(false)
            .build()

        val createDocumentResult = documentManager.createDocument("eu.europa.ec.eudi.pid.1", false)
        assertTrue(createDocumentResult is CreateDocumentResult.Success)

        val unsignedDocument =
            (createDocumentResult as CreateDocumentResult.Success).unsignedDocument

        val proofSigner = JWSProofSigner(unsignedDocument, SupportedProofAlgorithm.Jws.ES256)
        val algorithm = proofSigner.popSigner.algorithm

        assertTrue(proofSigner.popSigner.bindingKey is JwtBindingKey.Jwk)
        val publicKey = proofSigner.popSigner.bindingKey as JwtBindingKey.Jwk

        val header = JWSHeader.Builder(algorithm).apply {
            type(JOSEObjectType("openid4vci-proof+jwt"))
            jwk(publicKey.jwk)
        }.build()

        val claimsSet = JWTClaimsSet.Builder().apply {
            audience("https://example.openid4vci.com")
            claim("nonce", CNonce("1234567890").value)
            issueTime(Date.from(Instant.now()))
        }.build()

        val signedJWT = SignedJWT(header, claimsSet)
        signedJWT.sign(proofSigner)

        val verified = signedJWT.verify(ECDSAVerifier(publicKey.jwk.toECKey()))

        assertTrue(verified)

        assertEquals(ProofSigner.UserAuthStatus.NotRequired, proofSigner.userAuthStatus)

        documentManager.deleteDocumentById(unsignedDocument.id)
    }
}