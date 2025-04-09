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

package eu.europa.ec.eudi.wallet

import android.content.Context
import androidx.annotation.RawRes
import com.android.identity.android.securearea.AndroidKeystoreSecureArea
import com.android.identity.android.storage.AndroidStorageEngine
import com.android.identity.securearea.SecureArea
import com.android.identity.securearea.SecureAreaRepository
import com.android.identity.storage.StorageEngine
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.engagement.BleRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.sample.SampleDocumentManager
import eu.europa.ec.eudi.wallet.internal.LogPrinterImpl
import eu.europa.ec.eudi.wallet.internal.i
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.presentation.PresentationManager
import eu.europa.ec.eudi.wallet.presentation.PresentationManagerImpl
import eu.europa.ec.eudi.wallet.statium.DocumentStatusResolver
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.TransactionsDecorator
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequestProcessor
import io.ktor.client.HttpClient
import kotlinx.io.files.Path
import java.io.File
import java.security.cert.X509Certificate
import com.android.identity.util.Logger as IdentityLogger

/**
 * The main entry point for interacting with the wallet
 *
 * To create an instance of [EudiWallet], use the [EudiWallet.invoke] method or the [Builder] class.
 *
 * @see [Builder]
 * @see [EudiWallet.invoke]
 *
 * @property config the configuration object
 * @property documentManager the document manager
 * @property presentationManager the presentation manager for both proximity and remote presentation
 * @property transferManager the transfer manager for proximity presentation
 * @property logger the logger
 * @property documentStatusResolver
 */
interface EudiWallet : SampleDocumentManager, PresentationManager, DocumentStatusResolver {

    val config: EudiWalletConfig
    val documentManager: DocumentManager
    val presentationManager: PresentationManager
    val transferManager: TransferManager
    val logger: Logger
    val documentStatusResolver: DocumentStatusResolver

    /**
     * Enumerate the secure areas available in the wallet
     * @return a list of secure area identifiers
     */
    fun enumerateSecureAreas(): List<String> =
        secureAreaRepository.implementations.map { it.identifier }

    /**
     * Sets the reader trust store with the given [ReaderTrustStore]. This method is useful when
     * the reader trust store is not set in the configuration object, or when the reader trust store
     * needs to be updated at runtime.
     * @param readerTrustStore the reader trust store
     * @return this [EudiWallet] instance
     */
    fun setReaderTrustStore(readerTrustStore: ReaderTrustStore): EudiWallet

    /**
     * Sets the reader trust store with the given list of [X509Certificate]. This method is useful
     * when the reader trust store is not set in the configuration object, or when the reader trust
     * store needs to be updated at runtime.
     *
     * @param readerCertificates the list of reader certificates
     * @return this [EudiWallet] instance
     */
    fun setTrustedReaderCertificates(trustedReaderCertificates: List<X509Certificate>): EudiWallet

    /**
     * Sets the reader trust store with the given list of raw resource IDs. This method is useful
     * when the reader trust store is not set in the configuration object, or when the reader trust
     * store needs to be updated at runtime.
     *
     * @param rawRes the list of raw resource IDs
     * @return this [EudiWallet] instance
     */
    fun setTrustedReaderCertificates(@RawRes vararg rawRes: Int): EudiWallet

    /**
     * Create an instance of [OpenId4VciManager] for the wallet to interact with the OpenId4Vci service
     *
     * @return an instance of [OpenId4VciManager]
     */
    fun createOpenId4VciManager(): OpenId4VciManager

    /**
     * Resolve the status of the document with the given [documentId]
     *
     * This method will return the status of the document if it is an [IssuedDocument]
     *
     * @param documentId the document ID
     * @return the status of the document
     */
    suspend fun resolveStatusById(documentId: DocumentId): Result<Status> {
        val document = getDocumentById(documentId) as? IssuedDocument
            ?: return Result.failure(IllegalStateException("Document is not an IssuedDocument"))
        return resolveStatus(document)
    }

    companion object {

        private const val TAG = "EudiWallet"

        /**
         * Create an instance of [EudiWallet] with the given configuration and additional configuration
         * using the [Builder] class
         *
         * @param context application context
         * @param config the configuration object
         * @param extraConfiguration additional configuration to be applied based on the [Builder]
         */
        operator fun invoke(
            context: Context,
            config: EudiWalletConfig,
            extraConfiguration: (Builder.() -> Unit)? = null,
        ): EudiWallet {
            val builder = Builder(context, config)
            extraConfiguration?.invoke(builder)
            return builder.build()
        }
    }

    /**
     * Builder class to create an instance of [EudiWallet]
     * @param context application context
     * @param config the configuration object
     *
     * @property config the configuration object
     * @property storageEngine the storage engine to use for storing/retrieving documents if you want to provide a different implementation
     * @property secureAreas the secure areas to use for documents' keys management if you want to provide a different implementation
     * @property documentManager the document manager to use if you want to provide a custom implementation
     * @property readerTrustStore the reader trust store to use if you want to provide a custom implementation
     * @property presentationManager the presentation manager to use if you want to provide a custom implementation
     * @property logger the logger to use if you want to provide a custom implementation
     * @property ktorHttpClientFactory the Ktor HTTP client factory to use if you want to provide a custom implementation
     * @property transactionLogger the transaction logger to use if you want to provide a custom implementation
     * @property documentStatusResolver the document status resolver to use if you want to provide a custom implementation
     */
    class Builder(
        context: Context,
        val config: EudiWalletConfig,
    ) {
        private val context = context.applicationContext

        var storageEngine: StorageEngine? = null
        var secureAreas: List<SecureArea>? = null
        var documentManager: DocumentManager? = null
        var readerTrustStore: ReaderTrustStore? = null
        var presentationManager: PresentationManager? = null
        var logger: Logger? = null
        var ktorHttpClientFactory: (() -> HttpClient)? = null
        var transactionLogger: TransactionLogger? = null
        var documentStatusResolver: DocumentStatusResolver? = null

        /**
         * Configure with the given [SecureArea] implementations to use for documents' keys management.
         * If not set, the default secure area will be used which is [AndroidKeystoreSecureArea].
         *
         * @param secureAreas the secure areas
         * @return this [Builder] instance
         */
        fun withSecureAreas(secureAreas: List<SecureArea>) = apply {
            this.secureAreas = secureAreas
        }

        /**
         * Configure with the given [StorageEngine] to use for storing/retrieving documents.
         * If not set, the default storage engine will be used which is [AndroidStorageEngine].
         *
         * @param storageEngine the storage engine
         * @return this [Builder] instance
         */
        fun withStorageEngine(storageEngine: StorageEngine) = apply {
            this.storageEngine = storageEngine
        }

        /**
         * Configure with the given [DocumentManager] to use. If not set, the default document manager
         * will be used which is [DocumentManagerImpl] configured with the provided [storageEngine] and [secureAreas]
         * if they are set.
         *
         * @param documentManager the document manager
         * @return this [Builder] instance
         */
        fun withDocumentManager(documentManager: DocumentManager) =
            apply { this.documentManager = documentManager }

        /**
         * Configure with the given [ReaderTrustStore] to use for performing reader authentication.
         * If not set, the default reader trust store will be used which is initialized with the certificates
         * provided in the [EudiWalletConfig.configureReaderTrustStore] methods.
         *
         * @param readerTrustStore the reader trust store
         * @return this [Builder] instance
         */
        fun withReaderTrustStore(readerTrustStore: ReaderTrustStore) =
            apply { this.readerTrustStore = readerTrustStore }

        /**
         * Configure with the given [PresentationManager] to use for both proximity and remote presentation.
         * If not set, the default presentation manager will be used which is [PresentationManagerImpl]
         * that uses the [eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl] for proximity presentation
         * and [OpenId4VpManager] for remote presentation.
         *
         * @param presentationManager the presentation manager
         * @return this [Builder] instance
         */
        fun withPresentationManager(presentationManager: PresentationManager) =
            apply { this.presentationManager = presentationManager }

        /**
         * Configure with the given [Logger] to use for logging. If not set, the default logger will be used
         * which is configured with the [EudiWalletConfig.configureLogging].
         *
         * @param logger the logger
         * @return this [Builder] instance
         */
        fun withLogger(logger: Logger) = apply { this.logger = logger }

        /**
         * Configure with the given Ktor HTTP client factory to use for making HTTP requests.
         * Ktor HTTP client is used by the [OpenId4VpManager] and [OpenId4VciManager] for making HTTP requests.
         *
         * If not set, the default Ktor HTTP client factory will be used which is initialized with the default
         *
         * @param ktorHttpClientFactory the Ktor HTTP client factory
         * @return this [Builder] instance
         */
        fun withKtorHttpClientFactory(ktorHttpClientFactory: () -> HttpClient) = apply {
            this.ktorHttpClientFactory = ktorHttpClientFactory
        }


        /**
         * Configure with the given [TransactionLogger] to use for logging transactions.
         * If not set, the default transaction logger will be used which logs transactions to the console.
         *
         * @param transactionLogger the transaction logger
         * @return this [Builder] instance
         */
        fun withTransactionLogger(transactionLogger: TransactionLogger) = apply {
            this.transactionLogger = transactionLogger
        }

        /**
         * Configure with the given [DocumentStatusResolver] to use for resolving the status of documents.
         * If not set, the default document status resolver will be used which is
         * [eu.europa.ec.eudi.wallet.statium.DocumentStatusResolverImpl] that uses the [HttpClient] provided in the configuration.
         *
         * @param documentStatusResolver the document status resolver
         * @return this [Builder] instance
         */
        fun withDocumentStatusResolver(documentStatusResolver: DocumentStatusResolver) = apply {
            this.documentStatusResolver = documentStatusResolver
        }

        /**
         * Build the [EudiWallet] instance
         *
         * The [EudiWallet] instance will be created based on the configuration provided in the [Builder] class.
         *
         * The [EudiWallet] instance will be created with the following default implementations if not set:
         * - [AndroidStorageEngine] for storing/retrieving documents
         * - [AndroidKeystoreSecureArea] for managing documents' keys
         * - [DocumentManagerImpl] for managing documents
         * - [PresentationManagerImpl] for both proximity and remote presentation
         * - [OpenId4VpManager] for remote presentation
         * - [eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl] for proximity presentation
         * - [eu.europa.ec.eudi.wallet.statium.DocumentStatusResolverImpl] for resolving the status of documents
         *
         * **Note**: The [EudiWalletConfig.documentsStorageDir] is not set, the default storage directory
         * will be used which is the application's no backup files directory.
         *
         * **Note**: The [EudiWalletConfig.userAuthenticationRequired] is set to true and the device is not secured with a PIN,
         * pattern, or password, the configuration will be updated to set the user authentication required to false.
         *
         * **Note**: The [EudiWalletConfig.useStrongBoxForKeys] is set to true and the device does not support StrongBox,
         * the configuration will be updated to set the use StrongBox for keys to false.
         *
         * @return [EudiWallet]
         */
        fun build(): EudiWallet {

            ensureStrongBoxIsSupported()
            ensureUserAuthIsSupported()

            val documentManagerToUse =
                documentManager ?: getDefaultDocumentManager(storageEngine, secureAreas)

            val readerTrustStoreToUse = readerTrustStore ?: defaultReaderTrustStore

            val transferManager = getTransferManager(documentManagerToUse, readerTrustStoreToUse)

            val presentationManagerToUse = presentationManager ?: getDefaultPresentationManager(
                documentManager = documentManagerToUse,
                transferManager = transferManager,
                readerTrustStore = readerTrustStoreToUse
            ).wrapWithTrasactionLogger(documentManagerToUse)

            val openId4VciManagerFactory = {
                config.openId4VciConfig?.let { openId4VciConfig ->
                    OpenId4VciManager(context) {
                        documentManager(documentManagerToUse)
                        config(openId4VciConfig)
                        logger(loggerObj)
                        ktorHttpClientFactory?.let { ktorHttpClientFactory(it) }
                    }
                } ?: throw IllegalStateException("OpenId4Vp configuration is missing")
            }

            val documentStatusResolverToUse = getDocumentStatusResolver()

            return EudiWalletImpl(
                context = context,
                config = config,
                documentManager = documentManagerToUse,
                presentationManager = presentationManagerToUse,
                transferManager = transferManager,
                logger = loggerObj,
                readerTrustStoreConsumer = { presentationManagerToUse.readerTrustStore = it },
                openId4VciManagerFactory = openId4VciManagerFactory,
                transactionLogger = transactionLogger,
                documentStatusResolver = documentStatusResolverToUse
            )
        }

        /**
         * Get the default [PresentationManagerImpl] instance based on the [DocumentManager] and [TransferManager] implementations
         * @param documentManager the document manager
         * @param transferManager the transfer manager
         * @param readerTrustStore the reader trust store
         * @return the default [PresentationManagerImpl] instance
         */
        @JvmSynthetic
        internal fun getDefaultPresentationManager(
            documentManager: DocumentManager,
            transferManager: TransferManager,
            readerTrustStore: ReaderTrustStore?,
        ): PresentationManagerImpl {
            val openId4vpManager = config.openId4VpConfig?.let { openId4VpConfig ->
                OpenId4VpManager(
                    config = openId4VpConfig,
                    requestProcessor = OpenId4VpRequestProcessor(documentManager, readerTrustStore),
                    logger = loggerObj,
                    ktorHttpClientFactory = ktorHttpClientFactory
                )
            }
            return PresentationManagerImpl(
                transferManager = transferManager,
                openId4vpManager = openId4vpManager,
                nfcEngagementServiceClass = config.nfcEngagementServiceClass,
            )
        }

        @get:JvmSynthetic
        internal val loggerObj: Logger by lazy {
            (logger ?: Logger(config)).also {
                IdentityLogger.setLogPrinter(LogPrinterImpl(it))
            }
        }

        @get:JvmSynthetic
        internal val defaultStorageDir: Path
            get() = Path(
                File(
                    context.noBackupFilesDir,
                    "${config.documentManagerIdentifier}.bin"
                ).path
            )

        /**
         * Get the default [ReaderTrustStore] instance based on the certificates provided in the configuration
         * @return the default [ReaderTrustStore] instance
         */
        @get:JvmSynthetic
        internal val defaultReaderTrustStore: ReaderTrustStore?
            get() = config.readerTrustedCertificates?.let { certificates ->
                ReaderTrustStore.getDefault(certificates)
            }

        /**
         * Get the default [StorageEngine] instance based on the configuration
         * @return the default [StorageEngine] instance
         * @throws IllegalStateException if [EudiWalletConfig.documentsStorageDir] is not set
         */
        @get:JvmSynthetic
        internal val defaultStorageEngine: StorageEngine
            get() {
                val documentsStorageDirToUse = config.documentsStorageDir
                    ?.let { dir ->
                        when {
                            dir.isDirectory -> File(dir, "${config.documentManagerIdentifier}.bin")

                            else -> dir
                        }.path
                    }
                    ?.let { Path(it) }
                    ?: defaultStorageDir

                return AndroidStorageEngine.Builder(
                    context = context,
                    storageFile = documentsStorageDirToUse
                )
                    .setUseEncryption(config.encryptDocumentsInStorage)
                    .build()
            }

        /**
         * Get the default [SecureArea] instance based on the [StorageEngine]
         * @param storageEngine the storage engine
         * @return the default [SecureArea] instance
         */
        @JvmSynthetic
        internal fun getDefaultSecureArea(storageEngine: StorageEngine): SecureArea {
            return AndroidKeystoreSecureArea(context, storageEngine)
        }

        /**
         * Get the default [DocumentManager] instance based on the [StorageEngine] and [SecureArea] implementations
         * @param storageEngine the storage engine
         * @param secureAreas the secure areas
         * @return the default [DocumentManager] instance
         */
        @JvmSynthetic
        internal fun getDefaultDocumentManager(
            storageEngine: StorageEngine? = null,
            secureAreas: List<SecureArea>? = null,
        ): DocumentManager {
            val storageEngineToUse = storageEngine ?: defaultStorageEngine

            val secureAreaRepository = SecureAreaRepository().apply {
                val implementations = secureAreas.takeUnless { it.isNullOrEmpty() }
                    ?: setOf(getDefaultSecureArea(storageEngineToUse))

                implementations.forEach { addImplementation(it) }
            }

            return DocumentManager {
                setStorageEngine(storageEngineToUse)
                setSecureAreaRepository(secureAreaRepository)
                setIdentifier(config.documentManagerIdentifier)
            }
        }

        /**
         * Get the default [TransferManager] instance based on the [DocumentManager] and [ReaderTrustStore]
         * @param documentManager the document manager
         * @param readerTrustStore the reader trust store
         * @return the default [TransferManager] instance
         */
        @JvmSynthetic
        internal fun getTransferManager(
            documentManager: DocumentManager,
            readerTrustStore: ReaderTrustStore? = null,
        ) = TransferManager.getDefault(
            context = context,
            documentManager = documentManager,
            readerTrustStore = readerTrustStore,
            retrievalMethods = listOf(
                BleRetrievalMethod(
                    peripheralServerMode = config.enableBlePeripheralMode,
                    centralClientMode = config.enableBleCentralMode,
                    clearBleCache = config.clearBleCache
                )
            )
        )

        /**
         * Get the default [DocumentStatusResolver] instance based on the configuration if not set
         *
         * @return the [DocumentStatusResolver] instance
         */
        @JvmSynthetic
        internal fun getDocumentStatusResolver(): DocumentStatusResolver {
            return documentStatusResolver ?: ktorHttpClientFactory?.let {
                DocumentStatusResolver(
                    ktorHttpClientFactory = it,
                    allowedClockSkew = config.documentStatusResolverClockSkew
                )
            } ?: DocumentStatusResolver(allowedClockSkew = config.documentStatusResolverClockSkew)
        }

        /**
         * Returns the capabilities of the Android Keystore Secure Area
         */
        @get:JvmSynthetic
        internal val capabilities: AndroidKeystoreSecureArea.Capabilities by lazy {
            AndroidKeystoreSecureArea.Capabilities(context)
        }

        /**
         * Check if user authentication is required and update the configuration
         * if the device is not secure and the configuration is set to require user authentication
         */
        @JvmSynthetic
        internal fun ensureUserAuthIsSupported() {
            if (capabilities.secureLockScreenSetup.not() && config.userAuthenticationRequired) {
                loggerObj.i(
                    TAG,
                    """User authentication is required but the device is not secured with a PIN, pattern, or password.
                    | Setting EudiWalletConfig.userAuthenticationRequired to false.""".trimMargin()
                )
                config.userAuthenticationRequired = false
            }
        }

        /**
         * Check if StrongBox is supported on the device and update the configuration
         * if it is not supported and the configuration is set to use StrongBox
         */
        @JvmSynthetic
        internal fun ensureStrongBoxIsSupported() {
            if (capabilities.strongBoxSupported.not() && config.useStrongBoxForKeys) {
                loggerObj.i(
                    TAG, """StrongBox is not supported on this device.
                    | Setting EudiWalletConfig.useStrongBoxForKeys to false.""".trimMargin()
                )
                config.useStrongBoxForKeys = false
            }
        }

        /**
         * Wrap the [PresentationManager] with a transaction logger
         *
         * @receiver [PresentationManager]
         * @param documentManager the document manager
         * @return [PresentationManager] wrapped with a transaction logger
         */
        internal fun PresentationManager.wrapWithTrasactionLogger(documentManager: DocumentManager): PresentationManager {
            return transactionLogger?.let { tl ->
                TransactionsDecorator(
                    delegate = this,
                    documentManager = documentManager,
                    transactionLogger = tl,
                    logger = loggerObj,
                )
            } ?: this
        }

    }
}