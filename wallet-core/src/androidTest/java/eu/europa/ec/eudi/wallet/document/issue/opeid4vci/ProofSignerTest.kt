/*
 *  Copyright (c) 2023 European Commission
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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.openid4vci.BindingKey
import eu.europa.ec.eudi.openid4vci.CNonce
import eu.europa.ec.eudi.wallet.document.Constants.EU_PID_DOCTYPE
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.ProofSigner
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ProofSignerTest {

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
        val documentManager = DocumentManager.Builder(context)
            .enableUserAuth(true)
            .build()

        val issuanceRequestResult = documentManager.createIssuanceRequest(EU_PID_DOCTYPE, false)
        Assert.assertTrue(issuanceRequestResult is CreateIssuanceRequestResult.Success)

        val issuanceRequest =
            (issuanceRequestResult as CreateIssuanceRequestResult.Success).issuanceRequest

        val proofSigner = ProofSigner(issuanceRequest, JWSAlgorithm.ES256.name)
        val algorithm = proofSigner.getAlgorithm()

        Assert.assertTrue(proofSigner.getBindingKey() is BindingKey.Jwk)
        val publicKey = (proofSigner.getBindingKey() as BindingKey.Jwk).jwk

        val header = JWSHeader.Builder(algorithm).apply {
            type(JOSEObjectType("openid4vci-proof+jwt"))
            jwk(publicKey.toPublicJWK())
        }.build()

        val claimsSet = JWTClaimsSet.Builder().apply {
            audience("https://example.openid4vci.com")
            claim("nonce", CNonce("1234567890").value)
            issueTime(Date.from(Instant.now()))
        }.build()

        try {
            proofSigner.sign(header, claimsSet.toPayload().toBytes())
            Assert.fail("Expected UserAuthRequiredException")
        } catch (e: ProofSigner.UserAuthRequiredException) {
            Assert.assertTrue(proofSigner.userAuthRequired is ProofSigner.UserAuthRequired.Yes)
        } finally {
            documentManager.deleteDocumentById(issuanceRequest.documentId)
        }
    }

    @Test
    fun test_sign_method_creates_a_valid_signature() {
        val documentManager = DocumentManager.Builder(context)
            .enableUserAuth(false)
            .build()

        val issuanceRequestResult = documentManager.createIssuanceRequest(EU_PID_DOCTYPE, false)
        Assert.assertTrue(issuanceRequestResult is CreateIssuanceRequestResult.Success)

        val issuanceRequest =
            (issuanceRequestResult as CreateIssuanceRequestResult.Success).issuanceRequest

        val proofSigner = ProofSigner(issuanceRequest, JWSAlgorithm.ES256.name)
        val algorithm = proofSigner.getAlgorithm()

        Assert.assertTrue(proofSigner.getBindingKey() is BindingKey.Jwk)
        val publicKey = (proofSigner.getBindingKey() as BindingKey.Jwk).jwk

        val header = JWSHeader.Builder(algorithm).apply {
            type(JOSEObjectType("openid4vci-proof+jwt"))
            jwk(publicKey.toPublicJWK())
        }.build()

        val claimsSet = JWTClaimsSet.Builder().apply {
            audience("https://example.openid4vci.com")
            claim("nonce", CNonce("1234567890").value)
            issueTime(Date.from(Instant.now()))
        }.build()

        val signedJWT = SignedJWT(header, claimsSet)
        signedJWT.sign(proofSigner)

        val verified = signedJWT.verify(ECDSAVerifier(publicKey.toECKey()))

        Assert.assertTrue(verified)

        Assert.assertEquals(ProofSigner.UserAuthRequired.No, proofSigner.userAuthRequired)

        documentManager.deleteDocumentById(issuanceRequest.documentId)
    }
}