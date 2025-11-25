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

package eu.europa.ec.eudi.wallet.provider

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.openid4vci.ClientAttestationJWT
import eu.europa.ec.eudi.openid4vci.ClientAttestationPoPJWTSpec
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.wallet.issue.openid4vci.javaAlgorithm
import org.multipaz.securearea.KeyInfo

open class WalletAttestationKey(
    val keyInfo: KeyInfo,
    val signFunction: suspend (ByteArray) -> ByteArray,
) {

    suspend fun WalletAttestationsProvider.toClientAuthentication(): Result<ClientAuthentication.AttestationBased> =
        runCatching {
            val attestation = getWalletAttestation(keyInfo)
                .map { SignedJWT.parse(it) }
                .getOrThrow()
            val algorithm = checkNotNull(keyInfo.algorithm.javaAlgorithm) {
                "Algorithm not found for wallet attestation key"
            }
            ClientAuthentication.AttestationBased(
                attestationJWT = ClientAttestationJWT(attestation),
                popJwtSpec = ClientAttestationPoPJWTSpec(
                    signer = object : Signer<JWK> {
                        override val javaAlgorithm: String = algorithm

                        override suspend fun acquire(): SignOperation<JWK> {
                            return SignOperation<JWK>(
                                function = { signFunction(it) },
                                publicMaterial = JWK.parse(keyInfo.publicKey.toJwk().toString())
                            )
                        }

                        override suspend fun release(signOperation: SignOperation<JWK>?) {
                            // nothing to release
                        }

                    }
                )
            )
        }

}