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
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vci.BindingKey
import eu.europa.ec.eudi.openid4vci.ProofType
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.SignedWithAuthKeyResult
import eu.europa.ec.eudi.openid4vci.ProofSigner as BaseProofSigner

internal class ProofSigner(
    private val algorithm: JWSAlgorithm,
    private val issuanceRequest: IssuanceRequest
) : BaseProofSigner {
    private val jwk = JWK.parseFromPEMEncodedObjects(issuanceRequest.publicKey.pem)
    var userAuthStatus: UserAuthStatus = UserAuthStatus.NotRequired
        private set

    override fun getAlgorithm(): JWSAlgorithm = algorithm

    override fun getBindingKey(): BindingKey = BindingKey.Jwk(jwk)

    override fun getJCAContext(): JCAContext = JCAContext()

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        userAuthStatus = UserAuthStatus.NotRequired
        val alg = AlgorithmsMap[header.algorithm] ?: throw JOSEException(
            AlgorithmSupportMessage.unsupportedJWSAlgorithm(
                header.algorithm,
                supportedJWSAlgorithms()
            )
        )
        return issuanceRequest.signWithAuthKey(signingInput, alg).let { signResult ->
            when (signResult) {
                is SignedWithAuthKeyResult.Success -> Base64URL.encode(signResult.signature.derToJose(algorithm))
                is SignedWithAuthKeyResult.Failure -> throw signResult.throwable
                is SignedWithAuthKeyResult.UserAuthRequired -> {
                    userAuthStatus = UserAuthStatus.Required(signResult.cryptoObject)
                    throw UserAuthRequiredException()
                }
            }
        }
    }

    override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> = AlgorithmsMap.keys.toMutableSet()

    companion object {
        val SupportedProofTypes = mapOf(
            ProofType.JWT to listOf(JWSAlgorithm.ES256)
        )
        val AlgorithmsMap = mapOf(
            JWSAlgorithm.ES256 to Algorithm.SHA256withECDSA
        )

        operator fun invoke(
            proofTypes: Map<ProofType, List<JWSAlgorithm>>,
            issuanceRequest: IssuanceRequest
        ): Result<ProofSigner> {
            val proofType = SupportedProofTypes.keys.firstOrNull { it in proofTypes.keys }
                ?: return Result.failure(IllegalArgumentException("Unsupported proof type"))
            val algorithms = SupportedProofTypes[proofType]
                ?.filter { it in (proofTypes[proofType] ?: emptyList()) }
                ?: return Result.failure(IllegalArgumentException("Unsupported algorithm for proof type"))
            val algorithm = algorithms.firstOrNull()
                ?: return Result.failure(IllegalArgumentException("No supported algorithm for proof type"))
            return Result.success(ProofSigner(algorithm, issuanceRequest))
        }
    }

    sealed interface UserAuthStatus {
        object NotRequired : UserAuthStatus
        data class Required(val cryptoObject: CryptoObject? = null) : UserAuthStatus
    }

    class UserAuthRequiredException : Throwable()
}