/*
 * Copyright (c) 2025 European Commission
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

import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.wallet.logging.Logger
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcPrivateKey


/**
 * DPoP signer implementation using Multipaz library.
 * Generates a new EC key pair for each instance.
 *
 * @property algorithm the signing algorithm to use
 */
@Deprecated("Use DPopSigner from dpop package")
class DPoPSigner private constructor(val algorithm: Algorithm) : Signer<JWK> {

    private val privateKey: EcPrivateKey by lazy {
        Crypto.createEcPrivateKey(algorithm.curve!!)
    }

    override val javaAlgorithm: String = algorithm.javaAlgorithm
        ?: throw IllegalArgumentException("Unsupported algorithm: $algorithm")

    override suspend fun acquire(): SignOperation<JWK> {
        val jwk = JWK.parse(privateKey.publicKey.toJwk().toString())
        return SignOperation(
            function = { input -> Crypto.sign(privateKey, algorithm, input).toDerEncoded() },
            publicMaterial = jwk
        )
    }

    override suspend fun release(signOperation: SignOperation<JWK>?) {
        // Nothing to release
    }

    companion object {
        operator fun invoke(
            algorithm: Algorithm = Algorithm.ESP256,
            logger: Logger? = null
        ): Result<DPoPSigner> {
            return runCatching {
                require(algorithm.isSigning) {
                    "Not supported algorithm: $algorithm"
                }
                requireNotNull(algorithm.curve) {
                    "Not supported algorithm: $algorithm"
                }

                DPoPSigner(algorithm)
            }.onFailure {
                logger?.log(
                    Logger.Record(
                        level = Logger.Companion.LEVEL_ERROR,
                        message = "Error creating DPoP signer",
                        thrown = it
                    )
                )
            }
        }
    }
}