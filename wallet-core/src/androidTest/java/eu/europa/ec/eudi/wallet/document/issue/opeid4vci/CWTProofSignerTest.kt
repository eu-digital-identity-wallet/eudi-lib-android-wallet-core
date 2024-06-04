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

import android.app.KeyguardManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.Constants.EU_PID_DOCTYPE
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.CWTProofSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.ProofSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.UserAuthRequiredException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.security.Security
import java.security.Signature
import java.util.*

@RunWith(AndroidJUnit4::class)
class CWTProofSignerTest {

    companion object {
        private lateinit var context: Context
        val BC = BouncyCastleProvider()

        @JvmStatic
        @BeforeClass
        @Throws(IOException::class)
        fun setUp() {
            Security.insertProviderAt(BC, 1)
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


        val issuanceRequestResult = documentManager.createIssuanceRequest(EU_PID_DOCTYPE, false)
        assertTrue(issuanceRequestResult is CreateIssuanceRequestResult.Success)

        val issuanceRequest =
            (issuanceRequestResult as CreateIssuanceRequestResult.Success).issuanceRequest

        val proofSigner = CWTProofSigner(issuanceRequest, CoseAlgorithm.ES256, CoseCurve.P_256)
        assertEquals(CoseAlgorithm.ES256, proofSigner.popSigner.algorithm)
        assertEquals(CoseCurve.P_256, proofSigner.popSigner.curve)

        assertTrue(proofSigner.popSigner.bindingKey is CwtBindingKey.CoseKey)

        val payload = "some random bytes".toByteArray()
        try {
            proofSigner.sign(payload)
            Assert.fail("Expected UserAuthRequiredException")
        } catch (e: UserAuthRequiredException) {
            assertTrue(proofSigner.userAuthStatus is ProofSigner.UserAuthStatus.Required)
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
        assertTrue(issuanceRequestResult is CreateIssuanceRequestResult.Success)

        val issuanceRequest =
            (issuanceRequestResult as CreateIssuanceRequestResult.Success).issuanceRequest

        val proofSigner = CWTProofSigner(issuanceRequest, CoseAlgorithm.ES256, CoseCurve.P_256)
        assertEquals(CoseAlgorithm.ES256, proofSigner.popSigner.algorithm)
        assertEquals(CoseCurve.P_256, proofSigner.popSigner.curve)

        assertTrue(proofSigner.popSigner.bindingKey is CwtBindingKey.CoseKey)

        val payload = "some random bytes".toByteArray()
        val proofSignature = proofSigner.sign(payload)
        val publicKey = (proofSigner.popSigner.bindingKey as CwtBindingKey.CoseKey).jwk.toECKey().toPublicKey()

        val result = Signature.getInstance("SHA256withECDSA", BC).apply {
            initVerify(publicKey)
            update(payload)
        }.verify(proofSignature)
        assertTrue(result)
        documentManager.deleteDocumentById(issuanceRequest.documentId)
    }
}