/*
 *  Copyright (c) 2023-2024 European Commission
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

import androidx.biometric.BiometricPrompt.CryptoObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.SignedWithAuthKeyResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.*
import eu.europa.ec.eudi.wallet.issue.openid4vci.ProofSigner
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class ProofSignerTest {

    private lateinit var issuanceRequest: IssuanceRequest

    @Before
    fun setup() {
        issuanceRequest = mockk(relaxed = true)
        every { issuanceRequest.publicKey } returns mockk(relaxed = true)
    }

    companion object {

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            mockkStatic(JWK::class)
            every { JWK.parseFromPEMEncodedObjects(any()) } returns mockk(relaxed = true)

        }
    }

    @Test
    fun `invoke returns JWTProofSigner when supported jwt proof type and algorithm are available`() {
        val issuerProofTypesSupported = ProofTypesSupported(
            setOf(
                ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256))
            )
        )

        val result = ProofSigner(issuanceRequest, issuerProofTypesSupported)
        assertTrue(result.isSuccess)
        val signer = result.getOrThrow()
        assertTrue(signer is JWSProofSigner)
    }

    @Test
    fun `invoke returns CWTProofSigner when supported cwt proof type and algorithm are available`() {
        val issuerProofTypesSupported = ProofTypesSupported(
            setOf(
                ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256))
            )
        )

        val result = ProofSigner(issuanceRequest, issuerProofTypesSupported)
        assertTrue(result.isSuccess)
        val signer = result.getOrThrow()
        assertTrue(signer is CWTProofSigner)
    }

    @Test(expected = UnsupportedProofTypeException::class)
    fun `invoke return failure result when no supported proof type is available`() {
        val issuerProofTypesSupported = ProofTypesSupported(
            setOf(
                ProofTypeMeta.LdpVp
            )
        )

        val result = ProofSigner(issuanceRequest, issuerProofTypesSupported)
        assertTrue(result.isFailure)
        result.getOrThrow()
    }

    @Test(expected = UnsupportedProofTypeException::class)
    fun `invoke returns failure result when no supported algorithm is available`() {
        val issuerProofTypesSupported = ProofTypesSupported(
            setOf(
                ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES384), listOf(CoseCurve.P_384))
            )
        )

        val result = ProofSigner(issuanceRequest, issuerProofTypesSupported)
        assertTrue(result.isFailure)
        result.getOrThrow()
    }

    @Test
    fun `doSign returns the signature when issuanceRequest signWithAuthKey succeeds`() {
        val okInput = byteArrayOf()
        val okSignature = byteArrayOf()

        every {
            issuanceRequest.signWithAuthKey(
                okInput,
                Algorithm.SHA256withECDSA
            )
        } returns SignedWithAuthKeyResult.Success(okSignature)

        val signer = object : ProofSigner() {
            override val popSigner: PopSigner = mockk(relaxed = true)
        }
        val signature = signer.doSign(issuanceRequest, okInput, Algorithm.SHA256withECDSA)
        assertSame(okSignature, signature)
    }

    @Test
    fun `doSign throws UserAuthRequiredException when issuanceRequest signWithAuthKey requires auth`() {
        val userAuthRequiredInput = byteArrayOf()
        val cryptoObject = mockk<CryptoObject>()
        every {
            issuanceRequest.signWithAuthKey(
                userAuthRequiredInput,
                Algorithm.SHA256withECDSA
            )
        } returns SignedWithAuthKeyResult.UserAuthRequired(cryptoObject)

        val signer = object : ProofSigner() {
            override val popSigner: PopSigner = mockk(relaxed = true)
        }
        assertEquals(ProofSigner.UserAuthStatus.NotRequired, signer.userAuthStatus)
        assertThrows(UserAuthRequiredException::class.java) {
            signer.doSign(issuanceRequest, userAuthRequiredInput, Algorithm.SHA256withECDSA)
        }
        assertTrue(signer.userAuthStatus is ProofSigner.UserAuthStatus.Required)
        val status = signer.userAuthStatus as ProofSigner.UserAuthStatus.Required
        assertSame(cryptoObject, status.cryptoObject)
    }

    @Test
    fun `doSign throws exception when issuanceRequest signWithAuthKey fails`() {
        val errorInput = byteArrayOf()
        val exception = mockk<Exception>()
        every {
            issuanceRequest.signWithAuthKey(
                errorInput,
                Algorithm.SHA256withECDSA
            )
        } returns SignedWithAuthKeyResult.Failure(exception)

        val signer = object : ProofSigner() {
            override val popSigner: PopSigner = mockk(relaxed = true)
        }
        val e = assertThrows(Exception::class.java) {
            signer.doSign(issuanceRequest, errorInput, Algorithm.SHA256withECDSA)
        }
        assertSame(exception, e)
    }
}