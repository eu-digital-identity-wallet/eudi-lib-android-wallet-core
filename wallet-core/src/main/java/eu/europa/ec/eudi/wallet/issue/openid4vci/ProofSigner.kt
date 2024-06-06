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
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.PopSigner
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
    fun doSign(
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
     */
    companion object {

        /**
         * Creates a proof signer for the given issuance request and credential configuration.
         * @param issuanceRequest The issuance request.
         * @param credentialConfiguration The credential configuration.
         * @param supportedProofTypesPrioritized The supported proof types prioritized.
         * @return The proof signer or a failure if the proof type is not supported.
         */
        @JvmStatic
        operator fun invoke(
            issuanceRequest: IssuanceRequest,
            credentialConfiguration: CredentialConfiguration,
            supportedProofTypesPrioritized: List<OpenId4VciManager.Config.ProofType>? = null,
        ): Result<ProofSigner> {
            return try {
                Result.success(
                    SupportedProofType
                        .apply { supportedProofTypesPrioritized?.let { prioritize(it) } }
                        .selectProofType(credentialConfiguration)
                        .createProofSigner(issuanceRequest)
                )
            } catch (e: Throwable) {
                Result.failure(e)
            }
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