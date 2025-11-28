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

import eu.europa.ec.eudi.openid4vci.KeyAttestationJWT
import eu.europa.ec.eudi.openid4vci.Nonce
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import org.multipaz.securearea.KeyLockedException
import org.multipaz.securearea.KeyUnlockData

class KeyAttestationSigner internal constructor(
    override val javaAlgorithm: String,
    val signer: ProofOfPossessionSigner,
    private val keyAttestationJWT: KeyAttestationJWT,
    private val keyUnlockData: KeyUnlockData? = null,
) : Signer<KeyAttestationJWT> {

    var keyLockedException: KeyLockedException? = null
        private set

    override suspend fun acquire(): SignOperation<KeyAttestationJWT> {

        return SignOperation(
            function = { input ->
                try {
                    signer.signPoP(input, keyUnlockData).toDerEncoded()
                } catch (e: KeyLockedException) {
                    keyLockedException = e
                    throw e
                }
            },
            publicMaterial = keyAttestationJWT
        )
    }

    override suspend fun release(signOperation: SignOperation<KeyAttestationJWT>?) {
        // nothing to release
    }

    companion object {

        fun Factory(
            signers: List<ProofOfPossessionSigner>,
            keyIndex: Int,
            walletAttestationsProvider: WalletAttestationsProvider,
            keyUnlockData: Map<String, KeyUnlockData?>? = null,
        ): suspend (Nonce?) -> Result<KeyAttestationSigner> = { nonce ->
            runCatching {
                val keyAttestationJWT = walletAttestationsProvider.getKeyAttestation(
                    keys = signers.map { it.getKeyInfo() },
                    nonce = nonce
                ).map {
                    KeyAttestationJWT(it)
                }.getOrThrow()

                val signer = signers[keyIndex]
                val algorithm = signer.getKeyInfo().algorithm
                val javaAlgorithm = requireNotNull(algorithm.javaAlgorithm) {
                    "No JCA algorithm name for ${algorithm.name}"
                }
                val keyUnlockDataForSigner = keyUnlockData?.get(signer.keyAlias)

                KeyAttestationSigner(
                    javaAlgorithm,
                    signer,
                    keyAttestationJWT,
                    keyUnlockDataForSigner
                )
            }
        }
    }
}