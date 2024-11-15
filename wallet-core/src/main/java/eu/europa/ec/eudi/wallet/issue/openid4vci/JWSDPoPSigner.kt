/*
 * Copyright (c) 2024 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.android.identity.crypto.Algorithm
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcCurve
import com.android.identity.crypto.EcPrivateKey
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner


/**
 * A [JWSSigner] implementation for DPoP.
 * @property popSigner the DPoP signer [PopSigner.Jwt]
 * @property jcaContext the JCA context
 * @constructor Creates a DPoP signer.
 * @throws Exception If an error occurs during key generation.
 * @see JWSSigner
 */
internal class JWSDPoPSigner private constructor() : JWSSigner {

    private val privateKey: EcPrivateKey by lazy {
        Crypto.createEcPrivateKey(EcCurve.P256)
    }

    private val jcaContext = JCAContext()

    override fun getJCAContext(): JCAContext = jcaContext

    private val jwk: JWK
        get() = JWK.parseFromPEMEncodedObjects(privateKey.publicKey.toPem())

    private val jwsAlgorithm: JWSAlgorithm
        get() = JWSAlgorithm.parse(Algorithm.ES256.jwseAlgorithmIdentifier)

    val popSigner: PopSigner.Jwt
        get() = PopSigner.Jwt(
            algorithm = jwsAlgorithm,
            bindingKey = JwtBindingKey.Jwk(jwk),
            jwsSigner = this
        )

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        val signature = Crypto.sign(privateKey, Algorithm.ES256, signingInput)
        return Base64URL.encode(signature.toCoseEncoded())
    }

    override fun supportedJWSAlgorithms(): Set<JWSAlgorithm> = Algorithm.entries
        .mapNotNull {
            try {
                it.jwseAlgorithmIdentifier
            } catch (_: Throwable) {
                null
            }
        }
        .map { JWSAlgorithm.parse(it) }
        .toSet()

    /**
     * Companion object for the JWSDPoPSigner class.
     */
    companion object {
        /**
         * Creates a [PopSigner.Jwt] instance.
         * @return [Result<PopSigner.Jwt>] The result of the operation. If successful, the result contains the [PopSigner.Jwt] instance.
         * If unsuccessful, the result contains the exception that occurred.
         */
        operator fun invoke(): Result<PopSigner.Jwt> {
            return try {
                Result.success(JWSDPoPSigner().popSigner)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}