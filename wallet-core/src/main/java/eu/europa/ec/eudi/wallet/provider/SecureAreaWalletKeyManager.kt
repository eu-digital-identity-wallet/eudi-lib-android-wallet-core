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

import eu.europa.ec.eudi.wallet.internal.asProvider
import kotlinx.coroutines.withContext
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.UnlockReason
import java.security.MessageDigest

/**
 * A generic implementation of [WalletKeyManager] that delegates cryptographic operations
 * to a provided [SecureArea].
 *
 * This implementation enforces privacy by deriving a stable key alias from the [issuerUrl]
 * (using SHA-256). This ensures that a unique key is used for each Authorization Server.
 *
 * Checks if a key exists in the [SecureArea] for that alias.
 * If it exists and matches a supported algorithm, it is reused.
 * If it does not exist or the algorithm is incompatible, a new key is generated.
 *
 * @param secureArea The underlying secure storage abstraction.
 * @param createKeySettingsProvider A lambda that provides configuration for creating new keys
 * given a selected [Algorithm].
 * @param keyUnlockDataProvider Optional provider for user-authentication data (e.g., Biometrics/PIN)
 * if the key requires unlocking before use. Defaults to null.
 */
open class SecureAreaWalletKeyManager(
    private val secureArea: SecureArea,
    private val createKeySettingsProvider: suspend (Algorithm) -> CreateKeySettings,
    private val keyUnlockDataProvider: suspend (String, SecureArea) -> KeyUnlockData? = { _, _ -> null },
) : WalletKeyManager {

    override suspend fun getOrCreateWalletAttestationKey(
        issuerUrl: String,
        supportedAlgorithms: List<Algorithm>,
    ): Result<WalletAttestationKey> = runCatching {
        val keyAlias = generateKeyAlias(issuerUrl)

        val keyInfo: KeyInfo = try {
            // get existing key info if available and compatible
            // otherwise an exception is thrown
            secureArea.getKeyInfo(keyAlias).takeIf {
                it.algorithm in supportedAlgorithms
            } ?: throw IllegalArgumentException("Key algorithm not supported")

        } catch (_: IllegalArgumentException) {
            // create new key if not existing or not compatible
            val matchedAlgorithm = requireNotNull(
                secureArea.supportedAlgorithms.firstOrNull { it in supportedAlgorithms }
            ) {
                val walletSupports =
                    secureArea.supportedAlgorithms.joinToString(",") { it.name }
                val authSupports = supportedAlgorithms.joinToString(",") { it.name }
                "No suitable algorithm can be found! Wallet:[$walletSupports], authServer:[$authSupports]"
            }
            val createKeySettings = createKeySettingsProvider(matchedAlgorithm)
            secureArea.createKey(keyAlias, createKeySettings)
        }
        WalletAttestationKey(keyInfo) { data ->
            val keyUnlockData = keyUnlockDataProvider(keyAlias, secureArea)
            val provider = keyUnlockData.asProvider()
            withContext(provider) {
                secureArea.sign(keyAlias, data, UnlockReason.Unspecified).toDerEncoded()
            }
        }
    }

    override suspend fun getWalletAttestationKey(keyAlias: String): WalletAttestationKey? {
        return runCatching {
            secureArea.getKeyInfo(keyAlias)
        }.map { keyInfo ->
            WalletAttestationKey(keyInfo) { data ->
                val keyUnlockData = keyUnlockDataProvider(keyAlias, secureArea)
                val provider = keyUnlockData.asProvider()
                withContext(provider) {
                    secureArea.sign(keyAlias, data, UnlockReason.Unspecified).toDerEncoded()
                }
            }
        }.getOrNull()
    }

    /**
     * Generates a privacy-preserving alias for the key based on the target URL.
     * This ensures that different Authorization Servers get different keys.
     */
    private fun generateKeyAlias(issuerUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(issuerUrl.toByteArray())
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }.take(16)
        return "client-attestation-$hashHex"
    }
}