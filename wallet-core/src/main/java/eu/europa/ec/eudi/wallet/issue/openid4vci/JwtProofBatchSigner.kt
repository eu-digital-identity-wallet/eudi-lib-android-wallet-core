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
import eu.europa.ec.eudi.openid4vci.BatchSignOperation
import eu.europa.ec.eudi.openid4vci.BatchSigner
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import org.multipaz.securearea.KeyLockedException
import org.multipaz.securearea.KeyUnlockData

/**
 * A [BatchSigner] that wraps wallet [ProofOfPossessionSigner] instances to produce
 * JWT proofs without key attestation. Each signer produces one [SignOperation] that
 * signs with the credential's private key and exposes the public key as a [JwtBindingKey.Jwk].
 */
internal class JwtProofBatchSigner(
    override val javaAlgorithm: String,
    private val signers: List<ProofOfPossessionSigner>,
    private val keyUnlockData: Map<String, KeyUnlockData?>? = null,
) : BatchSigner<JwtBindingKey> {

    var keyLockedException: KeyLockedException? = null
        private set

    override suspend fun authenticate(): BatchSignOperation<JwtBindingKey> {
        return BatchSignOperation(
            signers.map { signer ->
                val keyInfo = signer.getKeyInfo()
                val jwk = JWK.parse(keyInfo.publicKey.toJwk().toString())
                val unlockData = keyUnlockData?.get(signer.keyAlias)
                SignOperation(
                    function = { input ->
                        try {
                            signer.signPoP(input, unlockData).toDerEncoded()
                        } catch (e: KeyLockedException) {
                            keyLockedException = e
                            throw e
                        }
                    },
                    publicMaterial = JwtBindingKey.Jwk(jwk)
                )
            }
        )
    }

    override suspend fun release(signOps: BatchSignOperation<JwtBindingKey>?) {
        // No resources to release
    }
}
