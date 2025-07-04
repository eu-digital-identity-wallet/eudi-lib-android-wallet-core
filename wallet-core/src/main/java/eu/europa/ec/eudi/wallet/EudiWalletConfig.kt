/*
 * Copyright (c) 2023-2025 European Commission
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
import eu.europa.ec.eudi.wallet.EudiWalletConfig.Companion.DEFAULT_DOCUMENT_MANAGER_IDENTIFIER
import eu.europa.ec.eudi.wallet.dcapi.DCAPIConfig
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.internal.getCertificate
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import java.security.cert.X509Certificate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Eudi wallet config. This config is used to configure the default settings of the Eudi wallet.
 *
 * Custom configuration and implementations of the various components can be provided using the
 * [EudiWallet.Builder] class.
 *
 * Example usage:
 *
 * ```
 * val storageFile = File(applicationContext.noBackupFilesDir.path, "main.db")
 * val config = EudiWalletConfig()
 *     .configureDocumentManager(storageFile.absolutePath)
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
 *     .configureDCAPI {
 *         withEnabled(true) // Enable DCAPI, by default it is disabled
 *         withPrivilegedAllowlist("allowlist") // your own allowlist of privileged browsers/apps that you trust
 *     }
 *
 * ```
 *
 * @property openId4VciConfig the OpenID4VCI configuration
 * @property openId4VpConfig the OpenID4VP configuration
 * @property dcapiConfig the DCAPI configuration
 * @property documentManagerIdentifier the document manager identifier
 * @property documentsStoragePath the documents storage path
 * @property enableBlePeripheralMode whether to enable BLE peripheral mode
 * @property enableBleCentralMode whether to enable BLE central mode
 * @property clearBleCache whether to clear the BLE cache
 * @property logLevel the log level
 * @property logSizeLimit the log size limit
 * @property readerTrustedCertificates the reader trusted certificates
 * @property userAuthenticationRequired whether user authentication is required
 * @property userAuthenticationTimeout the user authentication timeout
 * @property useStrongBoxForKeys whether to use the strong box for keys
 * @property documentStatusResolverClockSkew the clock skew for the document status resolver
 *
 * @see EudiWallet.Builder
 */

class EudiWalletConfig {
    /**
     * Configuration for OpenID4VCI operations. This can be set using [configureOpenId4Vci] methods.
     * When null, OpenID4VCI functionality requires configuration to be passed directly to methods that use it,
     * such as [EudiWallet.createOpenId4VciManager].
     */
    var openId4VciConfig: OpenId4VciManager.Config? = null
        private set

    /**
     * Configure OpenID for Verifiable Credential Issuance (OpenID4VCI).
     * This configuration is used by [EudiWallet.createOpenId4VciManager] when no specific config is provided.
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
     * Configure OpenID for Verifiable Credential Issuance (OpenID4VCI) using a builder pattern.
     * This configuration is used by [EudiWallet.createOpenId4VciManager] when no specific config is provided.
     *
     * @see OpenId4VciManager.Config
     * @see OpenId4VciManager.Config.Builder
     *
     * @param openId4VciConfig the OpenID4VCI configuration lambda with [OpenId4VciManager.Config.Builder] as receiver
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

    /**
     * Configuration for the Digital Credential.
     */
    var dcapiConfig: DCAPIConfig? = null
        private set

    /**
     * Configure the DCAPI.
     *
     * @see DCAPIConfig
     * @see DCAPIConfig.Builder
     *
     * @param dcapiConfig the DCAPI configuration
     * @return the [EudiWalletConfig] instance
     */
    fun configureDCAPI(dcapiConfig: DCAPIConfig) = apply {
        this.dcapiConfig = dcapiConfig
    }

    /**
     * Configure the DCAPI using a [DCAPIConfig.Builder] as a lambda with receiver.
     *
     * @see DCAPIConfig
     * @see DCAPIConfig.Builder
     *
     * @param dcapiConfig the DCAPI configuration lambda
     * @return the [EudiWalletConfig] instance
     */
    fun configureDCAPI(dcapiConfig: DCAPIConfig.Builder.() -> Unit) = apply {
        this.dcapiConfig = DCAPIConfig.Builder().apply(dcapiConfig).build()
    }

    var documentManagerIdentifier: String = DEFAULT_DOCUMENT_MANAGER_IDENTIFIER
        private set
    var documentsStoragePath: String? = null
        internal set // internal for setting the default value from the builder

    /**
     * Configure the built-in document manager.
     *
     * Allowing to configure the documents storage path and the document manager identifier.
     * The default document manager identifier is set to
     * [DEFAULT_DOCUMENT_MANAGER_IDENTIFIER].
     *
     * @see eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
     * @see org.multipaz.storage.Storage
     *
     * @param storagePath the documents storage path
     * @param identifier the document manager identifier
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureDocumentManager(
        storagePath: String,
        identifier: String? = null
    ) = apply {
        documentsStoragePath = storagePath
        identifier?.let { documentManagerIdentifier = it }
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
        internal set // internal for setting the default value from the builder
    var userAuthenticationTimeout: Long = 0L
        private set
    var useStrongBoxForKeys: Boolean = true
        internal set // internal for setting the default value from the builder

    /**
     * Configure the document key creation. This allows to configure if user authentication is
     * required to unlock key usage, the user authentication timeout and whether to use the
     * strong box for keys.
     * These values are used to create the [eu.europa.ec.eudi.wallet.document.CreateDocumentSettings]
     * using [eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings]
     * method.
     *
     * **Note**: when setting userAuthenticationRequired to true, device must be secured with a PIN, pattern
     * or password.
     *
     * **Note**: when setting useStrongBoxForKeys to true, the device must support the StrongBox.
     *
     * The default values are:
     * - userAuthenticationRequired: false
     * - userAuthenticationTimeout: 0
     * - useStrongBoxForKeys: true if supported by the device
     *
     * @param userAuthenticationRequired whether user authentication is required
     * @param userAuthenticationTimeout  If 0, user authentication is required for every use of the
     * key, otherwise it's required within the given amount of milliseconds
     * @param useStrongBoxForKeys whether to use the strong box for keys
     */
    fun configureDocumentKeyCreation(
        userAuthenticationRequired: Boolean = false,
        userAuthenticationTimeout: Long = 0L,
        useStrongBoxForKeys: Boolean = true,
    ) = apply {
        this.userAuthenticationRequired = userAuthenticationRequired
        this.userAuthenticationTimeout = userAuthenticationTimeout
        this.useStrongBoxForKeys = useStrongBoxForKeys

        if (this.userAuthenticationRequired) {
            require(this.userAuthenticationTimeout >= 0) { "User authentication timeout must be equal or greater than 0" }
        }
    }

    var documentStatusResolverClockSkew: Duration = Duration.ZERO
        private set

    /**
     * Configure the document status resolver clock skew. This allows to configure the clock skew for
     * the provided document status resolver.
     */
    fun configureDocumentStatusResolver(clockSkewInMinutes: Long) = apply {
        this.documentStatusResolverClockSkew = clockSkewInMinutes.minutes
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

