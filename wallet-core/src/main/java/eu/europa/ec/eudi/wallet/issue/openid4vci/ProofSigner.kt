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

import androidx.biometric.BiometricPrompt.CryptoObject
import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.SignedWithAuthKeyResult

internal abstract class ProofSigner {
    abstract val popSigner: PopSigner
    var userAuthStatus: UserAuthStatus = UserAuthStatus.NotRequired
        protected set

    protected fun doSign(
        issuanceRequest: IssuanceRequest,
        signingInput: ByteArray,
        @Algorithm algorithm: String
    ): ByteArray {
        userAuthStatus = UserAuthStatus.NotRequired
        return issuanceRequest.signWithAuthKey(signingInput, algorithm).let { signResult ->
            when (signResult) {
                is SignedWithAuthKeyResult.Success -> signResult.signature
                is SignedWithAuthKeyResult.Failure -> throw signResult.throwable
                is SignedWithAuthKeyResult.UserAuthRequired -> {
                    userAuthStatus = UserAuthStatus.Required(signResult.cryptoObject)
                    throw UserAuthRequiredException()
                }
            }
        }
    }

    companion object Factory {
        val SupportedProofTypes = mapOf(
            ProofType.JWT to ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256)),
            ProofType.CWT to ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256))
        )

        fun selectSupportedProofType(issuerSupportedProofTypes: ProofTypesSupported): ProofTypeMeta? {
            return issuerSupportedProofTypes.values
                .firstOrNull { it.type() in SupportedProofTypes.keys }
                ?.let { proofType ->
                    when (proofType.type()) {
                        ProofType.JWT -> selectSupportedAlgorithm(proofType as ProofTypeMeta.Jwt)?.let { proofType }
                        ProofType.CWT -> selectSupportedAlgorithm(proofType as ProofTypeMeta.Cwt)?.let { proofType }
                        ProofType.LDP_VP -> null
                    }
                }
        }

        fun selectSupportedAlgorithm(metadata: ProofTypeMeta.Jwt): JWSAlgorithm? {
            val supportedType = SupportedProofTypes[ProofType.JWT] as ProofTypeMeta.Jwt
            return supportedType.algorithms.firstOrNull { it in metadata.algorithms }
        }

        fun selectSupportedAlgorithm(metadata: ProofTypeMeta.Cwt): Pair<CoseAlgorithm, CoseCurve>? {
            val supportedType = SupportedProofTypes[ProofType.CWT] as ProofTypeMeta.Cwt
            return supportedType.algorithms.firstOrNull { it in metadata.algorithms }
                ?.let { algorithm ->
                    supportedType.curves.firstOrNull { it in metadata.curves }
                        ?.let { curve -> algorithm to curve }
                }

        }

        operator fun invoke(
            issuanceRequest: IssuanceRequest,
            credentialConfiguration: CredentialConfiguration,
        ) = invoke(issuanceRequest, credentialConfiguration.proofTypesSupported)

        operator fun invoke(
            issuanceRequest: IssuanceRequest,
            issuerSupportedProofTypes: ProofTypesSupported,
        ): Result<ProofSigner> {
            val selectedProofType = selectSupportedProofType(issuerSupportedProofTypes)
                ?: return Result.failure(UnsupportedProofTypeException())


            return when (selectedProofType) {
                is ProofTypeMeta.Jwt -> selectSupportedAlgorithm(selectedProofType)?.let { alg ->
                    JWSProofSigner(issuanceRequest, alg)
                }

                is ProofTypeMeta.Cwt -> selectSupportedAlgorithm(selectedProofType)?.let { (alg, crv) ->
                    CWTProofSigner(issuanceRequest, alg, crv)
                }

                else -> null
            }?.let { Result.success(it) }
                ?: Result.failure(UnsupportedProofTypeException())
        }
    }

    sealed interface UserAuthStatus {
        object NotRequired : UserAuthStatus
        data class Required(val cryptoObject: CryptoObject? = null) : UserAuthStatus
    }
}