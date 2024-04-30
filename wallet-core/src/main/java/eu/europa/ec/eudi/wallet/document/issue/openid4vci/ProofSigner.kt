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

package eu.europa.ec.eudi.wallet.document.issue.openid4vci

import android.util.Log
import androidx.biometric.BiometricPrompt.CryptoObject
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vci.BindingKey
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.SignedWithAuthKeyResult
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.PublicKey
import eu.europa.ec.eudi.openid4vci.ProofSigner as BaseProofSigner


internal class ProofSigner(
    private val issuanceRequest: IssuanceRequest,
    private val algorithm: String,
) : BaseProofSigner {

    private val jwk = JWK.parseFromPEMEncodedObjects(issuanceRequest.publicKey.pem)
        .also {
            Log.d(TAG, "Document's PublicKey in JWK: ${it.toJSONString()}")
        }
    var userAuthRequired: UserAuthRequired = UserAuthRequired.No
        private set

    override fun getAlgorithm(): JWSAlgorithm = JWSAlgorithm.parse(algorithm)
    override fun getBindingKey(): BindingKey = BindingKey.Jwk(jwk)
    override fun getJCAContext(): JCAContext = JCAContext()

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        userAuthRequired = UserAuthRequired.No
        if (!supportedJWSAlgorithms().contains(header.algorithm)) {
            throw JOSEException(
                AlgorithmSupportMessage.unsupportedJWSAlgorithm(
                    header.algorithm,
                    supportedJWSAlgorithms()
                )
            )
        }
        return issuanceRequest.signWithAuthKey(signingInput, header.algorithm.coreAlg).let {
            when (it) {
                is SignedWithAuthKeyResult.Success -> Base64URL.encode(derToJose(it.signature))
                is SignedWithAuthKeyResult.Failure -> throw it.throwable
                is SignedWithAuthKeyResult.UserAuthRequired -> {
                    userAuthRequired = UserAuthRequired.Yes(it.cryptoObject)
                    throw UserAuthRequiredException()
                }
            }
        }
    }

    override fun supportedJWSAlgorithms(): Set<JWSAlgorithm> = algorithmMap.keys.toSet()

    private val JWSAlgorithm.coreAlg: String
        get() = algorithmMap[this] ?: throw JOSEException("Unknown algorithm: $this")

    private val PublicKey.pem: String
        get() = StringWriter().use { wr ->
            PemWriter(wr).use { pwr ->
                pwr.writeObject(PemObject("PUBLIC KEY", this.encoded))
                pwr.flush()
            }
            wr.toString()
        }

    private fun derToJose(bytes: ByteArray): ByteArray {
        val len = ECDSA.getSignatureByteArrayLength(getAlgorithm())
        return ECDSA.transcodeSignatureToConcat(bytes, len)
    }

    companion object {
        private const val TAG = "ProofSigner"
        private val algorithmMap = mapOf(
            JWSAlgorithm.ES256 to Algorithm.SHA256withECDSA,
        )

        internal val SUPPORTED_ALGORITHMS = algorithmMap.keys.toSet()
    }

    internal sealed interface UserAuthRequired {
        object No : UserAuthRequired
        data class Yes(val cryptoObject: CryptoObject? = null) : UserAuthRequired
    }

    internal class UserAuthRequiredException : Exception()
}