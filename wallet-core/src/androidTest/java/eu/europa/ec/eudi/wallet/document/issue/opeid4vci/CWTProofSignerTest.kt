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

import COSE.AlgorithmID
import COSE.Message
import COSE.MessageTag
import COSE.OneKey
import android.app.KeyguardManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.openid4vci.*
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
import org.junit.Ignore
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


        val issuanceRequestResult = documentManager.createIssuanceRequest("eu.europa.ec.eudiw.pid.1", false)
        assertTrue(issuanceRequestResult is CreateIssuanceRequestResult.Success)

        val issuanceRequest =
            (issuanceRequestResult as CreateIssuanceRequestResult.Success).issuanceRequest

        val proofSigner = CWTProofSigner(issuanceRequest, SupportedProofAlgorithm.Cose.ES256_P_256)
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


        val issuanceRequestResult = documentManager.createIssuanceRequest("eu.europa.ec.eudiw.pid.1", false)
        assertTrue(issuanceRequestResult is CreateIssuanceRequestResult.Success)

        val issuanceRequest =
            (issuanceRequestResult as CreateIssuanceRequestResult.Success).issuanceRequest

        val proofSigner = CWTProofSigner(issuanceRequest, SupportedProofAlgorithm.Cose.ES256_P_256)
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

    @Ignore("This is not for CI/CD use. This is just to test the sign1 message")
    @Test
    fun test_doSign_when_used_in_sign1_message_is_verified() {
        val documentManager = DocumentManager.Builder(context)
            .enableUserAuth(false)
            .build()

        val issuanceRequestResult = documentManager.createIssuanceRequest("eu.europa.ec.eudiw.pid.1", false)
        assertTrue(issuanceRequestResult is CreateIssuanceRequestResult.Success)

        val issuanceRequest =
            (issuanceRequestResult as CreateIssuanceRequestResult.Success).issuanceRequest

        val cwtSigner = CWTProofSigner(issuanceRequest, SupportedProofAlgorithm.Cose.ES256_P_256)
        val oneKey = OneKey(issuanceRequest.publicKey, null).AsCBOR()
        val protectedHeaders = CBORObject.NewMap()
            .Add(CBORObject.FromObject(1), AlgorithmID.ECDSA_256.AsCBOR())
            .Add(CBORObject.FromObject(3), "openid4vci-proof+cwt")
            .Add("COSE_Key", oneKey)
            .EncodeToBytes()
        val unProtectedHeaders = CBORObject.NewMap()
        val payload = byteArrayOf(1, 2, 3)
        val structureToSign = CBORObject.NewArray()
            .Add(protectedHeaders)
            .Add(payload)
            .EncodeToBytes()
        val signature = cwtSigner.sign(structureToSign)

        val sign1Bytes = CBORObject.FromObjectAndTag(
            CBORObject.NewArray().apply {
                Add(protectedHeaders)
                Add(unProtectedHeaders)
                Add(payload)
                Add(signature)
            }, MessageTag.Sign1.value
        ).EncodeToBytes()

        val sign1 = Message.DecodeFromBytes(sign1Bytes, MessageTag.Sign1) as COSE.Sign1Message

        sign1.validate(OneKey(sign1.protectedAttributes.get("COSE_Key")))
    }
}