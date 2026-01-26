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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.storage.android.AndroidStorage
import java.security.SecureRandom

/**
 * The default Android implementation of [WalletKeyManager].
 *
 * This class handles the initialization of an [AndroidKeystoreSecureArea] backed by
 * a dedicated file storage in the application's `noBackupFilesDir`.
 *
 * @param context The Android Application Context.
 */
class DefaultWalletKeyManager(
    private val context: Context,
) : WalletKeyManager {


    override suspend fun getOrCreateWalletAttestationKey(
        issuerUrl: String,
        supportedAlgorithms: List<Algorithm>,
    ): Result<WalletAttestationKey> {
        return getSecureAreaWalletKeyManager().getOrCreateWalletAttestationKey(
            issuerUrl,
            supportedAlgorithms
        )
    }

    override suspend fun getWalletAttestationKey(keyAlias: String): WalletAttestationKey? {
        return getSecureAreaWalletKeyManager().getWalletAttestationKey(keyAlias)
    }

    private var secureAreaBased: SecureAreaWalletKeyManager? = null
    private val mutex = Mutex()

    private suspend fun getSecureAreaWalletKeyManager(): SecureAreaWalletKeyManager {
        return secureAreaBased ?: mutex.withLock {
            val storage = AndroidStorage("${context.noBackupFilesDir.path}/wallet-attest.bin")
            val secureArea = AndroidKeystoreSecureArea.create(storage)

            SecureAreaWalletKeyManager(secureArea, createKeySettingsProvider = { algorithm ->
                val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
                AndroidKeystoreCreateKeySettings.Builder(ByteString(challenge))
                    .setAlgorithm(algorithm)
                    .build()
            }).also { secureAreaBased = it }

        }
    }
}