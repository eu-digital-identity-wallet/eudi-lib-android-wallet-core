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

import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea
import java.security.MessageDigest

open class SecureAreaWalletKeyManager(
    private val secureArea: SecureArea,
    private val createKeySettingsProvider: suspend (Algorithm) -> CreateKeySettings,
    private val keyUnlockDataProvider: suspend (String, SecureArea) -> KeyUnlockData? = { _, _ -> null },
) : WalletKeyManager {

    override suspend fun getWalletAttestationKey(
        authorizationServerUrl: String,
        supportedAlgorithms: List<Algorithm>,
    ): Result<WalletAttestationKey> = runCatching {
        val keyAlias = generateKeyAlias(authorizationServerUrl)

        val keyInfo: KeyInfo = try {
            // get existing key info if available and compatible
            // otherwise an exception is thrown
            secureArea.getKeyInfo(keyAlias).takeIf {
                it.algorithm in supportedAlgorithms
            } ?: throw IllegalArgumentException("Key algorithm not supported")

        } catch (_: IllegalArgumentException) {
            // create new key if not existing or not compatible
            val matchedAlgorithm =
                secureArea.supportedAlgorithms.first { it in supportedAlgorithms }
            val createKeySettings = createKeySettingsProvider(matchedAlgorithm)
            secureArea.createKey(keyAlias, createKeySettings)
        }
        WalletAttestationKey(keyInfo) { data ->
            val keyUnlockData = keyUnlockDataProvider(keyAlias, secureArea)
            secureArea.sign(keyAlias, data, keyUnlockData).toDerEncoded()
        }
    }

    private fun generateKeyAlias(authorizationServerUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(authorizationServerUrl.toByteArray())
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }.take(16)
        return "client-attestation-$hashHex"
    }
}