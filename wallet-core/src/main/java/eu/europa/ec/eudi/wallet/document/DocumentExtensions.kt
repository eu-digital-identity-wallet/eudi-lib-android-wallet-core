/*
 * Copyright (c) 2024-2025 European Commission
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

import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy.OneTimeUse
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy.RotateUse
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreKeyUnlockData
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.UserAuthenticationType
import java.security.SecureRandom
import kotlin.math.min

/**
 * Provides extension functions for [Document] and [EudiWallet] related to document management.
 * This object includes methods for retrieving default key unlock data and create document settings.
 */
object DocumentExtensions {
    /**
     * Returns the default [AndroidKeystoreKeyUnlockData] for the [Document] instance.
     * The default key unlock data is based on the [Document.keyAlias].
     * This is applicable only if the document's key requires user authentication.
     *
     * @receiver the [Document] instance.
     * @return The default [AndroidKeystoreKeyUnlockData] for the [Document] instance if document requires user authentication, otherwise `null`.
     * @throws IllegalStateException if the [Document] is not managed by [AndroidKeystoreSecureArea].
     * @see AndroidKeystoreKeyUnlockData
     * @see Document
     */
    @get:JvmName("getDefaultKeyUnlockData")
    @get:Throws(IllegalStateException::class)
    @get:JvmStatic
    @Deprecated("Use getDefaultKeyUnlockData(document) instead")
    val Document.DefaultKeyUnlockData: AndroidKeystoreKeyUnlockData?
        get() = runBlocking {
            when (this@DefaultKeyUnlockData) {
                is IssuedDocument -> getDefaultKeyUnlockData(this@DefaultKeyUnlockData)
                else -> null
            }
        }

    /**
     * Returns the default [AndroidKeystoreKeyUnlockData] for the [IssuedDocument].
     * The default key unlock data is based on the [IssuedDocument.findCredential]
     *
     * @receiver The [IssuedDocument] instance.
     * @return The default [AndroidKeystoreKeyUnlockData] for the [IssuedDocument] if it requires user authentication, otherwise `null`.
     * @see getDefaultKeyUnlockData
     * @throws IllegalArgumentException if the document is not managed by [AndroidKeystoreSecureArea].
     */
    suspend fun IssuedDocument.getDefaultKeyUnlockData(): AndroidKeystoreKeyUnlockData? {
        return getDefaultKeyUnlockData(this)
    }

    /**
     * Returns the default [AndroidKeystoreKeyUnlockData] for the given [IssuedDocument].
     * The key unlock data is retrieved based on the document's associated credential.
     *
     * @param document The [IssuedDocument] for which to retrieve key unlock data.
     * @return The [AndroidKeystoreKeyUnlockData] for the document if it requires user authentication, otherwise `null`.
     * @throws IllegalArgumentException if the document is not managed by [AndroidKeystoreSecureArea].
     * @see AndroidKeystoreKeyUnlockData
     * @see IssuedDocument
     */
    @JvmName("getDefaultKeyUnlockDataForDocument")
    suspend fun getDefaultKeyUnlockData(document: IssuedDocument): AndroidKeystoreKeyUnlockData? {
        return document.findCredential()?.let { credential ->
            val secureArea = credential.secureArea
            require(secureArea is AndroidKeystoreSecureArea) {
                "Document is not managed by AndroidKeystoreSecureArea"
            }
            getDefaultKeyUnlockData(secureArea, credential.alias)
        }
    }

    /**
     * Returns the default [AndroidKeystoreKeyUnlockData] for the given [DocumentId].
     * The default key unlock data is based on the [Document.keyAlias] of the found document.
     * This is applicable only if the document's key requires user authentication.
     *
     * @receiver The [EudiWallet] instance.
     * @param documentId The [DocumentId] of the document.
     * @return The default [AndroidKeystoreKeyUnlockData] for the given [DocumentId] if the document requires user authentication, otherwise `null`.
     * @throws NoSuchElementException if the document is not found by the [DocumentId].
     * @throws IllegalStateException if the [Document] is not managed by [AndroidKeystoreSecureArea].
     * @see AndroidKeystoreKeyUnlockData
     * @see Document
     */
    @JvmName("getDefaultKeyUnlockData")
    @Throws(NoSuchElementException::class, IllegalStateException::class)
    @JvmStatic
    fun EudiWallet.getDefaultKeyUnlockData(documentId: DocumentId): AndroidKeystoreKeyUnlockData? {
        return when (val document = getDocumentById(documentId) as? IssuedDocument) {
            null -> throw NoSuchElementException("Document not found")
            else -> runBlocking { getDefaultKeyUnlockData(document) }
        }
    }

    /**
     * Returns the default [AndroidKeystoreKeyUnlockData] for the given [SecureArea] and [keyAlias]
     * if the [secureArea] is an instance of [AndroidKeystoreSecureArea].
     * @param secureArea The [SecureArea] instance.
     * @param keyAlias The key alias.
     * @return The default [AndroidKeystoreKeyUnlockData] if the [secureArea] is an instance of [AndroidKeystoreSecureArea], otherwise `null`.
     */
    fun getDefaultKeyUnlockData(
        secureArea: SecureArea,
        keyAlias: String,
    ): AndroidKeystoreKeyUnlockData? {
        return (secureArea as? AndroidKeystoreSecureArea)?.let {
            AndroidKeystoreKeyUnlockData(it, keyAlias)
        }
    }

    /**
     * Returns the default [CreateDocumentSettings] for the [EudiWallet] instance.
     * The default settings are based on the [EudiWalletConfig] and the presence of an available
     * [AndroidKeystoreSecureArea] implementation.
     *
     * The number of credentials in the returned settings is limited to the [Offer.OfferedDocument.batchCredentialIssuanceSize],
     * ensuring compatibility with issuer capabilities.
     *
     * The [attestationChallenge] is generated using a [SecureRandom] instance if not provided.
     * The [configure] lambda can be used to further customize the [AndroidKeystoreCreateKeySettings].
     *
     * @receiver The [EudiWallet] instance.
     * @param offeredDocument The [Offer.OfferedDocument] for which to create the default settings.
     *                        Used to determine the maximum number of credentials allowed.
     * @param attestationChallenge The attestation challenge to use when creating the keys. If `null`, a random challenge will be generated.
     * @param numberOfCredentials The number of credentials to pre-generate for the document.
     *                           Will be limited to not exceed [Offer.OfferedDocument.batchCredentialIssuanceSize]. Defaults to 1.
     * @param credentialPolicy The policy for credential usage ([OneTimeUse] or [RotateUse]). Defaults to [RotateUse].
     * @param configure A lambda to further customize the [AndroidKeystoreCreateKeySettings].
     *                 If not provided, settings will use values from [EudiWalletConfig].
     * @return The default [CreateDocumentSettings] configured for the offered document.
     * @throws NoSuchElementException if no [AndroidKeystoreSecureArea] implementation is available.
     * @see AndroidKeystoreCreateKeySettings.Builder
     * @see AndroidKeystoreCreateKeySettings
     * @see AndroidKeystoreSecureArea
     * @see CreateDocumentSettings
     */
    @JvmName("getDefaultCreateDocumentSettings")
    @Throws(NoSuchElementException::class)
    @JvmOverloads
    @JvmStatic
    fun EudiWallet.getDefaultCreateDocumentSettings(
        offeredDocument: Offer.OfferedDocument,
        attestationChallenge: ByteArray? = null,
        numberOfCredentials: Int = 1,
        credentialPolicy: CreateDocumentSettings.CredentialPolicy = RotateUse,
        configure: (AndroidKeystoreCreateKeySettings.Builder.() -> Unit)? = null,
    ): CreateDocumentSettings {
        // ensure the number of credentials is not greater than the batch size
        val numberOfCredentials =
            min(numberOfCredentials, offeredDocument.batchCredentialIssuanceSize)
        val secureAreaIdentifier = AndroidKeystoreSecureArea.IDENTIFIER
        val attestationChallengeToUse =
            attestationChallenge ?: SecureRandom().let { secureRandom ->
                ByteArray(32).also { secureRandom.nextBytes(it) }
            }
        val builder =
            AndroidKeystoreCreateKeySettings.Builder(ByteString(attestationChallengeToUse))
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

        return CreateDocumentSettings(
            secureAreaIdentifier = secureAreaIdentifier,
            createKeySettings = createKeySettings,
            numberOfCredentials = numberOfCredentials,
            credentialPolicy = credentialPolicy
        )
    }
}

