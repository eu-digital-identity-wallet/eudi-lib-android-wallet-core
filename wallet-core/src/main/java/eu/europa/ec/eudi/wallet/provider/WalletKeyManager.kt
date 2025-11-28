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
fun interface WalletKeyManager {
    /**
     * Retrieves or creates a signing key to be used for Wallet Attestation (Client Authentication).
     * The Wallet Attestation Keys must be distinct for different Authorization Servers but unique for a specific one, and
     * should be stored for subsequent use with the same Authorization Server
     *
     * The implementation should ensure that the returned key is compatible with one of the
     * [supportedAlgorithms] required by the Authorization Server.
     *
     * @param authorizationServerUrl The URL of the Authorization Server.
     *
     * @param supportedAlgorithms A list of cryptographic algorithms supported by the Authorization Server.
     * The returned key must use one of these algorithms.
     * @return A [Result] containing the [WalletAttestationKey], which includes the public key info
     * and a mechanism to sign data.
     */
    suspend fun getWalletAttestationKey(
        authorizationServerUrl: String,
        supportedAlgorithms: List<Algorithm>,
    ): Result<WalletAttestationKey>

    companion object {
        /**
         * Returns the default Android implementation backed by the Android Keystore.
         * @param context
         * @returns [DefaultWalletKeyManager] with use of [org.multipaz.securearea.AndroidKeystoreSecureArea]
         */
        fun getDefault(context: Context): WalletKeyManager = DefaultWalletKeyManager(context)
    }
}

