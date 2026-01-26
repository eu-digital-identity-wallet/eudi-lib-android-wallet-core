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

import android.content.Context
import org.multipaz.crypto.Algorithm

/**
 * Manages the cryptographic keys used for Client Authentication and Attestation binding.
 *
 * Responsible for creating, storing, and retrieving the cryptographic
 * keys that those attestations certify.
 */
interface WalletKeyManager {
    /**
     * Retrieves or creates a signing key to be used for Wallet Attestation (Client Authentication).
     * The implementation must ensure that keys are scoped to the specific Authorization Server
     * to prevent cross-service tracking (Unlinkability). The key alias is derived from the [issuerUrl].
     *
     * The implementation should ensure that the returned key is compatible with one of the
     * [supportedAlgorithms] required by the Authorization Server.
     *
     * @param issuerUrl The Issuer Identifier of the Authorization Server
     * This string is hashed to generate a unique, stable alias for the key in the Secure Area.
     *
     * @param supportedAlgorithms A list of cryptographic algorithms supported by the Authorization Server.
     * The returned key must use one of these algorithms.
     * @return A [Result] containing the [WalletAttestationKey], which includes the public key info
     * and a mechanism to sign data.
     */
    suspend fun getOrCreateWalletAttestationKey(
        issuerUrl: String,
        supportedAlgorithms: List<Algorithm>,
    ): Result<WalletAttestationKey>

    /**
     * Retrieves the existing Wallet Attestation Key for the specified Authorization Server URL.
     * If no key exists for the given Authorization Server, it returns null.
     * @param keyAlias The URL of the Authorization Server.
     * @return The existing [WalletAttestationKey] or null if not found.
     */
    suspend fun getWalletAttestationKey(
        keyAlias: String,
    ): WalletAttestationKey?

    companion object {
        /**
         * Returns the default Android implementation backed by the Android Keystore.
         * @param context
         * @returns [DefaultWalletKeyManager] with use of [org.multipaz.securearea.AndroidKeystoreSecureArea]
         */
        fun getDefault(context: Context): WalletKeyManager = DefaultWalletKeyManager(context)
    }
}

