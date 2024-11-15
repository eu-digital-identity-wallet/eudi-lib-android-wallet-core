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

package eu.europa.ec.eudi.wallet.document

import com.android.identity.android.securearea.AndroidKeystoreCreateKeySettings
import com.android.identity.android.securearea.AndroidKeystoreKeyUnlockData
import com.android.identity.android.securearea.AndroidKeystoreSecureArea
import com.android.identity.android.securearea.UserAuthenticationType
import eu.europa.ec.eudi.wallet.EudiWallet
import java.security.SecureRandom

object DocumentExtensions {
    /**
     * Returns the default [AndroidKeystoreKeyUnlockData] for the [Document] instance.
     * The default key unlock data is based on the [Document.keyAlias].
     * @see [AndroidKeystoreKeyUnlockData]
     * @see [Document]
     *
     * @receiver the [Document] instance
     * @return the default [AndroidKeystoreKeyUnlockData] for the [Document] instance
     */
    @get:JvmName("getDefaultKeyUnlockData")
    val Document.DefaultKeyUnlockData: AndroidKeystoreKeyUnlockData
        get() = AndroidKeystoreKeyUnlockData(keyAlias)

    /**
     * Returns the default [AndroidKeystoreKeyUnlockData] for the given [DocumentId].
     * The default key unlock data is based on the [Document.keyAlias].
     * @see [AndroidKeystoreKeyUnlockData]
     * @see [Document]
     *
     * @receiver the [EudiWallet] instance
     * @param documentId the [DocumentId] of the document
     * @return the default [AndroidKeystoreKeyUnlockData] for the given [DocumentId] or null if the document is not found
     */
    fun EudiWallet.getDefaultKeyUnlockData(documentId: DocumentId): AndroidKeystoreKeyUnlockData? {
        return getDocumentById(documentId)?.DefaultKeyUnlockData
    }

    /**
     * Returns the default [CreateDocumentSettings] for the [EudiWallet] instance.
     * The default settings are based on the [EudiWalletConfig] and the first available
     * [AndroidKeystoreSecureArea] implementation.
     * The [attestationChallenge] is generated using a [SecureRandom] instance.
     * The [configure] lambda can be used to further customize the [AndroidKeystoreCreateKeySettings].
     * If [secureAreaIdentifier] is not provided, the first available [AndroidKeystoreSecureArea] implementation
     * is used.
     * @throws NoSuchElementException if no [AndroidKeystoreSecureArea] implementation is available
     * @see [AndroidKeystoreCreateKeySettings.Builder]
     * @see [AndroidKeystoreCreateKeySettings]
     * @see [AndroidKeystoreSecureArea]
     * @see [CreateDocumentSettings]
     *
     * @receiver the [EudiWallet] instance
     * @param secureAreaIdentifier the [AndroidKeystoreSecureArea.identifier] where the document's keys should be stored
     * @param attestationChallenge the attestation challenge to use when creating the keys
     * @param configure a lambda to further customize the [AndroidKeystoreCreateKeySettings]
     */
    @JvmName("getDefaultCreateDocumentSettings")
    @Throws(NoSuchElementException::class)
    @JvmOverloads
    fun EudiWallet.getDefaultCreateDocumentSettings(
        secureAreaIdentifier: String? = null,
        attestationChallenge: ByteArray? = null,
        configure: (AndroidKeystoreCreateKeySettings.Builder.() -> Unit)? = null,
    ): CreateDocumentSettings {
        val attestationChallengeToUse = attestationChallenge ?: SecureRandom().let { secureRandom ->
            ByteArray(32).also { secureRandom.nextBytes(it) }
        }
        val builder = AndroidKeystoreCreateKeySettings.Builder(attestationChallengeToUse)
        val createKeySettings = when (configure) {
            null -> builder
                .setUseStrongBox(config.useStrongBoxForKeys)
                .setUserAuthenticationRequired(
                    required = config.userAuthenticationRequired,
                    timeoutMillis = config.userAuthenticationTimeout,
                    userAuthenticationTypes = setOf(
                        UserAuthenticationType.LSKF, UserAuthenticationType.BIOMETRIC
                    )
                )

            else -> builder.apply(configure)
        }.build()

        val secureAreaIdentifierToUse = secureAreaIdentifier ?: secureAreaRepository
            .implementations
            .first { it is AndroidKeystoreSecureArea }
            .identifier

        return CreateDocumentSettings(
            secureAreaIdentifier = secureAreaIdentifierToUse,
            createKeySettings = createKeySettings
        )
    }
}