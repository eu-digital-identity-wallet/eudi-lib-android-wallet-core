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
import com.nimbusds.jose.crypto.impl.ECDSA
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.CreateDocumentResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.CWTProofSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.ProofSigner
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
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofType.ProofAlgorithm as SupportedProofAlgorithm

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


        val unsignedDocumentResult = documentManager.createDocument("eu.europa.ec.eudi.pid.1", false)
        assertTrue(unsignedDocumentResult is CreateDocumentResult.Success)

        val unsignedDocument =
            (unsignedDocumentResult as CreateDocumentResult.Success).unsignedDocument

        val proofSigner = CWTProofSigner(unsignedDocument, SupportedProofAlgorithm.Cose.ES256_P_256)
        assertEquals(CoseAlgorithm.ES256, proofSigner.popSigner.algorithm)
        assertEquals(CoseCurve.P_256, proofSigner.popSigner.curve)

        assertTrue(proofSigner.popSigner.bindingKey is CwtBindingKey.CoseKey)

        val payload = "some random bytes".toByteArray()
        try {
            proofSigner.sign(payload)
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


        val unsignedDocumentResult = documentManager.createDocument("eu.europa.ec.eudi.pid.1", false)
        assertTrue(unsignedDocumentResult is CreateDocumentResult.Success)

        val unsignedDocument =
            (unsignedDocumentResult as CreateDocumentResult.Success).unsignedDocument

        val proofSigner = CWTProofSigner(unsignedDocument, SupportedProofAlgorithm.Cose.ES256_P_256)
        assertEquals(CoseAlgorithm.ES256, proofSigner.popSigner.algorithm)
        assertEquals(CoseCurve.P_256, proofSigner.popSigner.curve)

        assertTrue(proofSigner.popSigner.bindingKey is CwtBindingKey.CoseKey)

        val payload = "some random bytes".toByteArray()
        val proofSignature = proofSigner.sign(payload)
            .let { ECDSA.transcodeSignatureToDER(it) } // convert it back to DER
        val publicKey = (proofSigner.popSigner.bindingKey as CwtBindingKey.CoseKey).jwk.toECKey().toPublicKey()

        val result = Signature.getInstance("SHA256withECDSA", BC).apply {
            initVerify(publicKey)
            update(payload)
        }.verify(proofSignature)
        assertTrue(result)
        documentManager.deleteDocumentById(unsignedDocument.id)
    }
}