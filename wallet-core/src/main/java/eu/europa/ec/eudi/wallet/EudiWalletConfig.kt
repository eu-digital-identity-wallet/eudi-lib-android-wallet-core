/*
 * Copyright (c) 2023-2024 European Commission
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
@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet

import android.content.Context
import androidx.annotation.RawRes
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.wallet.internal.getCertificate
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import java.io.File
import java.security.cert.X509Certificate

/**
 * Eudi wallet config. This config is used to configure the default settings of the Eudi wallet.
 *
 * Custom configuration and implementations of the various components can be provided using the
 * [EudiWallet.Builder] class.
 *
 * Example usage:
 *
 * ```
 * val config = EudiWalletConfig()
 *     .configureDocumentManager(context.noBackupFilesDir)
 *     .configureLogging(
 *         // set log level to info
 *         level = Logger.LEVEL_INFO
 *     )
 *     .configureDocumentKeyCreation(
 *         // set userAuthenticationRequired to true to require user authentication
 *         userAuthenticationRequired = true,
 *         // set userAuthenticationTimeout to 30 seconds
 *         userAuthenticationTimeout = 30_000L,
 *         // set useStrongBoxForKeys to true to use the the device's StrongBox if available
 *         // to store the keys
 *         useStrongBoxForKeys = true
 *     )
 *     .configureReaderTrustStore(
 *         // set the reader trusted certificates for the reader trust store
 *         listOf(readerCertificate)
 *     )
 *     .configureOpenId4Vci {
 *         withIssuerUrl("https://issuer.com")
 *         withClientId("client-id")
 *         withAuthFlowRedirectionURI("eudi-openid4ci://authorize")
 *         withParUsage(OpenId4VciManager.Config.ParUsage.Companion.IF_SUPPORTED)
 *         withUseDPoPIfSupported(true)
 *     }
 *     .configureProximityPresentation(
 *         enableBlePeripheralMode = true,
 *         enableBleCentralMode = false,
 *         clearBleCache = true,
 *     )
 *     .configureOpenId4Vp {
 *         withEncryptionAlgorithms(
 *             EncryptionAlgorithm.ECDH_ES
 *         )
 *         withEncryptionMethods(
 *             EncryptionMethod.A128CBC_HS256,
 *             EncryptionMethod.A256GCM
 *         )
 *         withClientIdSchemes(
 *             ClientIdScheme.X509SanDns
 *         )
 *         withSchemes(
 *             "openid4vp",
 *             "eudi-openid4vp",
 *             "mdoc-openid4vp"
 *         )
 *     }
 *
 * ```
 *
 * @property openId4VciConfig the OpenID4VCI configuration
 * @property openId4VpConfig the OpenID4VP configuration
 * @property documentManagerIdentifier the document manager identifier
 * @property documentsStorageDir the documents storage directory
 * @property encryptDocumentsInStorage whether to encrypt documents in storage
 * @property enableBlePeripheralMode whether to enable BLE peripheral mode
 * @property enableBleCentralMode whether to enable BLE central mode
 * @property clearBleCache whether to clear the BLE cache
 * @property logLevel the log level
 * @property logSizeLimit the log size limit
 * @property readerTrustedCertificates the reader trusted certificates
 * @property userAuthenticationRequired whether user authentication is required
 * @property userAuthenticationTimeout the user authentication timeout
 * @property useStrongBoxForKeys whether to use the strong box for keys
 *
 * @see EudiWallet.Builder
 */

class EudiWalletConfig {
    var openId4VciConfig: OpenId4VciManager.Config? = null
        private set

    /**
     * Configure OpenID4VCI.
     *
     * @see OpenId4VciManager.Config
     * @see OpenId4VciManager.Config.Builder
     *
     * @param openId4VciConfig the OpenID4VCI configuration
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vci(openId4VciConfig: OpenId4VciManager.Config) = apply {
        this.openId4VciConfig = openId4VciConfig
    }

    /**
     * Configure OpenID4VCI using a [OpenId4VciManager.Config.Builder] as a lambda with receiver.
     *
     * @see OpenId4VciManager.Config
     * @see OpenId4VciManager.Config.Builder
     *
     * @param openId4VciConfig the OpenID4VCI configuration lambda
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vci(openId4VciConfig: OpenId4VciManager.Config.Builder.() -> Unit) = apply {
        this.openId4VciConfig = OpenId4VciManager.Config.Builder().apply(openId4VciConfig).build()
    }

    var openId4VpConfig: OpenId4VpConfig? = null
        private set

    /**
     * Configure OpenID4VP.
     *
     * @see OpenId4VpConfig
     * @see OpenId4VpConfig.Builder
     *
     * @param openId4VpConfig the OpenID4VP configuration
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vp(openId4VpConfig: OpenId4VpConfig) = apply {
        this.openId4VpConfig = openId4VpConfig
    }

    /**
     * Configure OpenID4VP using a [OpenId4VpConfig.Builder] as a lambda with receiver.
     *
     * @see OpenId4VpConfig
     * @see OpenId4VpConfig.Builder
     *
     * @param openId4VpConfig the OpenID4VP configuration lambda
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vp(openId4VpConfig: OpenId4VpConfig.Builder.() -> Unit) = apply {
        this.openId4VpConfig = OpenId4VpConfig.Builder().apply(openId4VpConfig).build()
    }

    var documentManagerIdentifier: String = DEFAULT_DOCUMENT_MANAGER_IDENTIFIER
        private set
    var documentsStorageDir: File? = null
        internal set // internal for setting the default value from the builder
    var encryptDocumentsInStorage: Boolean = true
        private set

    /**
     * Configure the built-in document manager.
     *
     * Allowing to configure the documents storage directory, the document manager identifier and
     * whether to encrypt documents in storage. The default document manager identifier is set to
     * [DEFAULT_DOCUMENT_MANAGER_IDENTIFIER].
     *
     * @see eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
     * @see com.android.identity.android.storage.AndroidStorageEngine
     *
     * @param storageDir the documents storage directory
     * @param identifier the document manager identifier
     * @param encryptDocuments whether to encrypt documents in storage
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureDocumentManager(
        storageDir: File,
        identifier: String? = null,
        encryptDocuments: Boolean = true,
    ) = apply {
        documentsStorageDir = storageDir
        identifier?.let { documentManagerIdentifier = it }
        encryptDocumentsInStorage = encryptDocuments
    }

    var enableBlePeripheralMode: Boolean = true
        private set
    var enableBleCentralMode: Boolean = false
        private set
    var clearBleCache: Boolean = true
        private set
    var nfcEngagementServiceClass: Class<out NfcEngagementService>? = null
        private set

    /**
     * Configure the proximity presentation. This allows to configure the BLE peripheral mode,
     * the BLE central mode and whether to clear the BLE cache. Also, it allows to set the NFC
     * engagement service class an implementation of [NfcEngagementService], which is used to
     * handle the NFC engagement.
     *
     * The default values are:
     * - enableBlePeripheralMode: true
     * - enableBleCentralMode: false
     * - clearBleCache: true
     * - nfcEngagementServiceClass: null
     *
     * @param enableBlePeripheralMode whether to enable BLE peripheral mode
     * @param enableBleCentralMode whether to enable BLE central mode
     * @param clearBleCache whether to clear the BLE cache
     * @param nfcEngagementServiceClass the NFC engagement service class
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureProximityPresentation(
        enableBlePeripheralMode: Boolean = true,
        enableBleCentralMode: Boolean = false,
        clearBleCache: Boolean = true,
        nfcEngagementServiceClass: Class<out NfcEngagementService>? = null,
    ) = apply {
        this.enableBlePeripheralMode = enableBlePeripheralMode
        this.enableBleCentralMode = enableBleCentralMode
        this.clearBleCache = clearBleCache
        this.nfcEngagementServiceClass = nfcEngagementServiceClass
    }

    @Logger.Level
    var logLevel: Int = Logger.LEVEL_INFO
        private set
    var logSizeLimit: Int = 1000
        private set

    /**
     * Configure the built-in logging. This allows to configure the log level and the log size limit.
     *
     * The default log level is set to [Logger.LEVEL_INFO] and the default log size limit is set to
     * 1000.
     *
     * @param level the log level
     * @param sizeLimit the log size limit
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureLogging(level: Int, sizeLimit: Int? = null) = apply {
        logLevel = level
        sizeLimit?.let { logSizeLimit = it }
    }

    var readerTrustedCertificates: List<X509Certificate>? = null
        private set

    /**
     * Configure the built-in [ReaderTrustStore]. This allows to set the reader trusted
     * certificates for the reader trust store.
     *
     * @param readerTrustedCertificates the reader trusted certificates
     * @return the [EudiWalletConfig] instance
     */
    fun configureReaderTrustStore(readerTrustedCertificates: List<X509Certificate>) = apply {
        this.readerTrustedCertificates = readerTrustedCertificates
    }

    /**
     * Configure the built-in [ReaderTrustStore]. This allows to set the reader trusted
     * certificates for the reader trust store.
     *
     * @param readerTrustedCertificates the reader trusted certificates
     * @return the [EudiWalletConfig] instance
     */
    fun configureReaderTrustStore(vararg readerTrustedCertificates: X509Certificate) = apply {
        this.readerTrustedCertificates = readerTrustedCertificates.toList()
    }

    /**
     * Configure the built-in [ReaderTrustStore].
     * This allows to set the reader trusted certificates for the reader trust store.
     * The certificates are loaded from the raw resources.
     *
     * @param context the context
     * @param certificateRes the reader trusted certificates raw resources
     * @return the [EudiWalletConfig] instance
     */
    fun configureReaderTrustStore(context: Context, @RawRes vararg certificateRes: Int) = apply {
        this.readerTrustedCertificates = certificateRes.map { context.getCertificate(it) }
    }

    var userAuthenticationRequired: Boolean = false
        private set
    var userAuthenticationTimeout: Long = 0L
        private set
    var useStrongBoxForKeys: Boolean = true
        private set

    /**
     * Configure the document key creation. This allows to configure if user authentication is
     * required to unlock key usage, the user authentication timeout and whether to use the
     * strong box for keys.
     * These values are used to create the [eu.europa.ec.eudi.wallet.document.CreateDocumentSettings]
     * using [eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings]
     * method.
     *
     * The default values are:
     * - userAuthenticationRequired: false
     * - userAuthenticationTimeout: 0
     * - useStrongBoxForKeys: true
     *
     * @param userAuthenticationRequired whether user authentication is required
     * @param userAuthenticationTimeout the user authentication timeout
     * @param useStrongBoxForKeys whether to use the strong box for keys
     */
    fun configureDocumentKeyCreation(
        userAuthenticationRequired: Boolean,
        userAuthenticationTimeout: Long,
        useStrongBoxForKeys: Boolean,
    ) = apply {
        require(userAuthenticationTimeout > 0) { "User authentication timeout must be greater than 0" }
        this.userAuthenticationRequired = userAuthenticationRequired
        this.userAuthenticationTimeout = userAuthenticationTimeout
        this.useStrongBoxForKeys = useStrongBoxForKeys
    }


    companion object {

        const val DEFAULT_DOCUMENT_MANAGER_IDENTIFIER = "EudiWalletDocumentManager"

        /**
         * Create a new EudiWalletConfig instance.
         * @param configure the configuration lambda
         * @return the EudiWalletConfig instance
         */
        operator fun invoke(configure: EudiWalletConfig.() -> Unit): EudiWalletConfig =
            EudiWalletConfig().apply(configure)
    }
}