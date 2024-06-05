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

/**
 * Abstract class for signing proofs.
 *
 * @property popSigner The signer for the proof of possession as required by the vci library.
 * @property userAuthStatus The status of the user authentication.
 */
internal abstract class ProofSigner {
    abstract val popSigner: PopSigner
    var userAuthStatus: UserAuthStatus = UserAuthStatus.NotRequired
        protected set

    /**
     * Signs the signing input with the authentication key from issuance request for the given algorithm.
     * @param issuanceRequest The issuance request.
     * @param signingInput The input to sign.
     * @param algorithm The algorithm to use for signing.
     * @throws UserAuthRequiredException If user authentication is required.
     * @throws Throwable If an error occurs during signing.
     * @return The signature of the signing input.
     */
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

    /**
     * Companion object for the ProofSigner class.
     * @property SupportedProofTypes The supported proof types.
     */
    companion object {
        val SupportedProofTypes = mapOf(
            ProofType.JWT to ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256)),
            ProofType.CWT to ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256))
        )

        /**
         * Selects the supported proof type from the issuer supported proof types.
         * @param issuerSupportedProofTypes The issuer supported proof types.
         * @return The supported proof type or null if none is supported.
         */
        @JvmStatic
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

        /**
         * Selects the supported algorithm from given JWT proof type metadata.
         * @param metadata The proof type metadata.
         * @return The supported algorithm or null if none is supported.
         */
        @JvmStatic
        fun selectSupportedAlgorithm(metadata: ProofTypeMeta.Jwt): JWSAlgorithm? {
            val supportedType = SupportedProofTypes[ProofType.JWT] as ProofTypeMeta.Jwt
            return supportedType.algorithms.firstOrNull { it in metadata.algorithms }
        }

        /**
         * Selects the supported algorithm and curve from given CWT proof type metadata.
         * @param metadata The proof type metadata.
         * @return The supported algorithm and curve or null if none is supported.
         */
        @JvmStatic
        fun selectSupportedAlgorithm(metadata: ProofTypeMeta.Cwt): Pair<CoseAlgorithm, CoseCurve>? {
            val supportedType = SupportedProofTypes[ProofType.CWT] as ProofTypeMeta.Cwt
            return supportedType.algorithms.firstOrNull { it in metadata.algorithms }
                ?.let { algorithm ->
                    supportedType.curves.firstOrNull { it in metadata.curves }
                        ?.let { curve -> algorithm to curve }
                }

        }

        /**
         * Creates a proof signer for the given issuance request and credential configuration.
         * @param issuanceRequest The issuance request.
         * @param credentialConfiguration The credential configuration.
         * @return The proof signer or a failure if the proof type is not supported.
         */
        @JvmStatic
        operator fun invoke(
            issuanceRequest: IssuanceRequest,
            credentialConfiguration: CredentialConfiguration,
        ) = invoke(issuanceRequest, credentialConfiguration.proofTypesSupported)

        /**
         * Creates a proof signer for the given issuance request and issuer supported proof types.
         * The proof signer is selected based on the issuer supported proof types and the [SupportedProofTypes]
         * of the [ProofSigner].
         * @param issuanceRequest The issuance request.
         * @param issuerSupportedProofTypes The issuer supported proof types.
         * @return The proof signer or a failure if the proof type is not supported.
         */
        @JvmStatic
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

                else -> null // should never happen. LDP_VP is not supported in [SupportedProofTypes]
            }?.let { Result.success(it) }
                ?: Result.failure(UnsupportedAlgorithmException())
        }
    }

    /**
     * Sealed interface for the user authentication status.
     */
    sealed interface UserAuthStatus {
        /**
         * User authentication is not required.
         */
        object NotRequired : UserAuthStatus

        /**
         * User authentication is required.
         * @property cryptoObject The crypto object to use for authentication.
         */
        data class Required(val cryptoObject: CryptoObject? = null) : UserAuthStatus
    }
}