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
package eu.europa.ec.eudi.wallet.issue.openid4vci

import COSE.AlgorithmID
import COSE.OneKey
import androidx.biometric.BiometricPrompt.CryptoObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.document.SignedWithAuthKeyResult
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Config.ProofType as ConfigProofType

class ProofSignerTest {

    private lateinit var unsignedDocument: UnsignedDocument
    private lateinit var credentialConfiguration: CredentialConfiguration

    @BeforeEach
    fun setup() {
        unsignedDocument = mockk(relaxed = true)
        credentialConfiguration = mockk<CredentialConfiguration>(relaxed = true)
        every { unsignedDocument.publicKey } returns mockk(relaxed = true)
    }

    companion object {

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            mockkStatic(JWK::class)
            every { JWK.parseFromPEMEncodedObjects(any()) } returns mockk(relaxed = true)

        }

        @JvmStatic
        fun proofTypesArgs() = listOf(
            listOf(listOf(ConfigProofType.CWT, ConfigProofType.JWT), CWTProofSigner::class.java),
            listOf(listOf(ConfigProofType.JWT, ConfigProofType.CWT), JWSProofSigner::class.java),
        ).map { Arguments.of(*it.toTypedArray()) }
    }

    @Test
    fun `invoke returns JWTProofSigner when supported jwt proof type and algorithm are available`() {
        every {
            credentialConfiguration.proofTypesSupported
        } returns ProofTypesSupported(setOf(ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256))))


        val result = ProofSigner(unsignedDocument, credentialConfiguration)
        assertTrue(result.isSuccess)
        val signer = result.getOrThrow()
        assertTrue(signer is JWSProofSigner)
    }

    @Test
    fun `invoke returns CWTProofSigner when supported cwt proof type and algorithm are available`() {
        every {
            credentialConfiguration.proofTypesSupported
        } returns ProofTypesSupported(
            setOf(
                ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256))
            )
        )

        val result = ProofSigner(unsignedDocument, credentialConfiguration)
        assertTrue(result.isSuccess)
        val signer = result.getOrThrow()
        assertTrue(signer is CWTProofSigner)
    }

    @Test
    fun `invoke return failure result when no supported proof type is available`() {
        every {
            credentialConfiguration.proofTypesSupported
        } returns ProofTypesSupported(
            setOf(
                ProofTypeMeta.LdpVp
            )
        )

        val result = ProofSigner(unsignedDocument, credentialConfiguration)
        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke returns failure result when no supported algorithm is available`() {
        every {
            credentialConfiguration.proofTypesSupported
        } returns ProofTypesSupported(
            setOf(
                ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES384), listOf(CoseCurve.P_384))
            )
        )

        val result = ProofSigner(unsignedDocument, credentialConfiguration)
        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke returns failure with UnsupportedProofTypeException when configured proof type is not supported`() {
        every {
            credentialConfiguration.proofTypesSupported
        } returns ProofTypesSupported(
            setOf(
                ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256))
            )
        )

        val result = ProofSigner(unsignedDocument, credentialConfiguration, listOf(ConfigProofType.JWT))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedProofTypeException)
    }

    @ParameterizedTest(name = "ordered proofTypes: {0} -> {1}")
    @MethodSource("proofTypesArgs")
    internal fun `invoke returns JWTProofSigner when prioritized over cwt proof type and algorithm are available`(
        orderedTypes: List<ConfigProofType>, signerClass: Class<ProofSigner>
    ) {
        val issuanceRequest = mockk<UnsignedDocument>(relaxed = true)
        val credentialConfiguration = mockk<CredentialConfiguration>(relaxed = true)
        every { issuanceRequest.publicKey } returns OneKey.generateKey(AlgorithmID.ECDSA_256).AsPublicKey()
        every {
            credentialConfiguration.proofTypesSupported
        } returns ProofTypesSupported(
            setOf(
                ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256)),
                ProofTypeMeta.Cwt(
                    listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256)
                )
            )
        )


        val result = ProofSigner(issuanceRequest, credentialConfiguration, orderedTypes)
        assertTrue(result.isSuccess)
        val signer = result.getOrThrow()
        assertTrue(signerClass.isAssignableFrom(signer::class.java))
    }

    @Test
    fun `doSign returns the signature when issuanceRequest signWithAuthKey succeeds`() {
        val okInput = byteArrayOf()
        val okSignature = byteArrayOf()

        every {
            unsignedDocument.signWithAuthKey(
                okInput,
                Algorithm.SHA256withECDSA
            )
        } returns SignedWithAuthKeyResult.Success(okSignature)

        val signer = object : ProofSigner() {
            override val popSigner: PopSigner = mockk(relaxed = true)
        }
        val signature = signer.doSign(unsignedDocument, okInput, Algorithm.SHA256withECDSA)
        assertSame(okSignature, signature)
    }

    @Test
    fun `doSign throws UserAuthRequiredException when issuanceRequest signWithAuthKey requires auth`() {
        val userAuthRequiredInput = byteArrayOf()
        val cryptoObject = mockk<CryptoObject>()
        every {
            unsignedDocument.signWithAuthKey(
                userAuthRequiredInput,
                Algorithm.SHA256withECDSA
            )
        } returns SignedWithAuthKeyResult.UserAuthRequired(cryptoObject)

        val signer = object : ProofSigner() {
            override val popSigner: PopSigner = mockk(relaxed = true)
        }
        assertEquals(ProofSigner.UserAuthStatus.NotRequired, signer.userAuthStatus)
        assertThrows(IllegalStateException::class.java) {
            signer.doSign(unsignedDocument, userAuthRequiredInput, Algorithm.SHA256withECDSA)
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
            unsignedDocument.signWithAuthKey(
                errorInput,
                Algorithm.SHA256withECDSA
            )
        } returns SignedWithAuthKeyResult.Failure(exception)

        val signer = object : ProofSigner() {
            override val popSigner: PopSigner = mockk(relaxed = true)
        }
        val e = assertThrows(Exception::class.java) {
            signer.doSign(unsignedDocument, errorInput, Algorithm.SHA256withECDSA)
        }
        assertSame(exception, e)
    }


}