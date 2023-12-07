/*
 * Copyright (c) 2023 European Commission
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.annotation.RawRes
import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.DocumentsResolver
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.retrieval.BleRetrievalMethod
import eu.europa.ec.eudi.wallet.document.AddDocumentResult
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DeleteDocumentResult
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.issue.IssueDocumentResult
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import eu.europa.ec.eudi.wallet.document.sample.SampleDocumentManager
import eu.europa.ec.eudi.wallet.internal.getCertificate
import eu.europa.ec.eudi.wallet.transfer.openid4vp.OpenId4vpManager
import java.security.cert.X509Certificate

/**
 * Eudi wallet sdk object to access the sdk functionalities. This object must be initialized before
 * using the sdk. The initialization can be done in the application class. The sdk can be initialized
 * with a [EudiWalletConfig] object.
 *
 * Initialize the sdk:
 * ```
 * val config = EudiWalletConfig.Builder(appContext)
 *    .storageDir(appContext.filesDir)
 *    .encryptDocumentsInStorage(true)
 *    .useHardwareToStoreKeys(true)
 *    .userAuthenticationRequired(true)
 *    .userAuthenticationTimeOut(30_000L)
 *    .trustedReaderCertificates(listof<X509Certificate>(
 *      // add trusted reader certificates
 *    ))
 *    .bleTransferMode(EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE)
 *    .build()
 * EudiWallet.init(context, config)
 * ```
 * @see [EudiWalletConfig] on how to configure the sdk
 *
 *
 *
 */
@SuppressLint("StaticFieldLeak")
object EudiWallet {

    @Volatile
    private lateinit var context: Context
    private lateinit var _config: EudiWalletConfig

    /**
     * Initialize the sdk with the given [config]
     *
     * @param context application context
     * @param config configuration object
     */
    fun init(context: Context, config: EudiWalletConfig) {
        this.context = context.applicationContext
        this._config = config
    }

    /**
     * The Config that used to initialize the sdk
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    val config get() = requireInit { _config }

    /**
     * Document manager
     * @see [DocumentManager]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    val documentManager: SampleDocumentManager by lazy {
        requireInit {
            val delegate = DocumentManager.Builder(context)
                .apply {
                    storageDir = config.documentsStorageDir
                    useEncryption = config.encryptDocumentsInStorage
                }
                .build() as DocumentManagerImpl

            delegate.userAuth(config.userAuthenticationRequired)
                .userAuthTimeout(config.userAuthenticationTimeOut)

            SampleDocumentManager.Builder(context)
                .apply { documentManager = delegate }
                .hardwareBacked(config.useHardwareToStoreKeys)
                .build()
        }
    }

    /**
     * Transfer manager
     * @see [TransferManager]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    val transferManager: TransferManager by lazy {
        requireInit {
            TransferManager.Builder(context)
                .apply {
                    _config.trustedReaderCertificates?.let {
                        readerTrustStore = ReaderTrustStore.getDefault(it)
                    }
                    retrievalMethods = deviceRetrievalMethods
                    documentsResolver = transferManagerDocumentsResolver
                }
                .build()
        }
    }

    fun issueDocument(docType: String, callback: (result: IssueDocumentResult) -> Unit) {
        requireInit {
            config.openId4VciConfig?.let { config ->
                OpenId4VciManager(context, config, documentManager)
                    .issueDocument(docType, callback)
            } ?: throw IllegalStateException("OpenId4VciConfig is not set in configuration")
        }
    }

    /**
     * OpenId4VP manager that can be used to verify OpenId4Vp requests
     * @see [OpenId4vpManager]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     * or if the [EudiWalletConfig.openId4VpVerifierApiUri] is not set
     */
    @get:Throws(IllegalStateException::class)
    val openId4vpManager: OpenId4vpManager by lazy {
        requireInit {
            config.openId4VpVerifierApiUri?.let {
                OpenId4vpManager(context, it, documentManager)
            } ?: throw IllegalStateException("OpenId4Vp verifier uri is not set in configuration")
        }
    }


    /**
     * Returns the list of documents
     * @see [DocumentManager.getDocuments]
     * @return the list of documents
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun getDocuments(): List<Document> = documentManager.getDocuments()

    /**
     * Returns the document with the given [documentId]
     * @param documentId the document's id
     * @see [DocumentManager.getDocumentById]
     * @return the document with the given [documentId] or null if not found
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun getDocumentById(documentId: DocumentId): Document? =
        documentManager.getDocumentById(documentId)

    /**
     * Delete the document with the given [documentId]
     * @param documentId the document's id
     * @see [DocumentManager.deleteDocumentById]
     * @return [DeleteDocumentResult]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun deleteDocumentById(documentId: DocumentId): DeleteDocumentResult =
        documentManager.deleteDocumentById(documentId)

    /**
     * Create an issuance request for the given [docType]
     * @param docType the docType of the document
     * @param hardwareBacked flag that indicates if the document's keys should be stored in hardware or not
     * @param attestationChallenge the attestation challenge to be used for the document's keys
     * attestation (optional). If not provided, the sdk will generate a random challenge
     * @see [DocumentManager.createIssuanceRequest]
     * @return [CreateIssuanceRequestResult]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun createIssuanceRequest(
        docType: String,
        hardwareBacked: Boolean,
        attestationChallenge: ByteArray? = null,
    ): CreateIssuanceRequestResult =
        documentManager.createIssuanceRequest(docType, hardwareBacked, attestationChallenge)

    /**
     * Add a document to the wallet
     * @param request the issuance request
     * @param data the document data provided by the issuer
     * @see [DocumentManager.addDocument]
     * @return [AddDocumentResult]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun addDocument(request: IssuanceRequest, data: ByteArray): AddDocumentResult =
        documentManager.addDocument(request, data)

    /**
     * Loads sample data into the wallet's document manager
     * @param sampleData the sample data
     * @see [SampleDocumentManager.loadSampleData]
     * @return [AddDocumentResult]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun loadSampleData(sampleData: ByteArray): LoadSampleResult =
        documentManager.loadSampleData(sampleData)

    /**
     * Sets the reader trust store with the readers' certificates that are trusted by the wallet
     * @param readerTrustStore the reader trust store
     * @see [TransferManager.setReaderTrustStore]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     * @return [EudiWallet]
     */
    fun setReaderTrustStore(readerTrustStore: ReaderTrustStore): EudiWallet {
        transferManager.setReaderTrustStore(readerTrustStore)
        return this
    }

    /**
     * Sets the readers' certificates that are trusted by the wallet
     * @param trustedReaderCertificates list of trusted reader certificates
     * @see [TransferManager.setReaderTrustStore]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     * @return [EudiWallet]
     */
    fun setTrustedReaderCertificates(trustedReaderCertificates: List<X509Certificate>) = apply {
        transferManager.setReaderTrustStore(ReaderTrustStore.getDefault(trustedReaderCertificates))
    }

    /**
     * Sets the readers' certificates from raw resources that are trusted by the wallet
     * @param rawRes list of raw resources of trusted reader certificates
     * @see [TransferManager.setReaderTrustStore]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     * @return [EudiWallet]
     */
    fun setTrustedReaderCertificates(@RawRes vararg rawRes: Int) = apply {
        setTrustedReaderCertificates(rawRes.map { context.getCertificate(it) })
    }

    /**
     * Adds a transfer event listener in order to be notified about transfer events
     * @see [TransferManager.addTransferEventListener]
     * @see [TransferEvent.Listener]
     * @param listener
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     * @return [EudiWallet]
     */
    fun addTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferManager.addTransferEventListener(listener)
    }

    /**
     * Removes a transfer event listener.
     * @see [TransferManager.removeTransferEventListener]
     * @see [TransferEvent.Listener]
     * @param listener
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     * @return [EudiWallet]
     *
     */
    fun removeTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferManager.removeTransferEventListener(listener)
    }

    /**
     * Removes all transfer event listeners.
     * @see [TransferManager.removeTransferEventListener]
     * @see [TransferEvent.Listener]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     * @return [EudiWallet]
     */
    fun removeAllTransferEventListeners() = apply {
        transferManager.removeAllTransferEventListeners()
    }

    /**
     * Starts the transfer process by engaging with the reader via QR code
     * @see [TransferManager.startQrEngagement]
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun startQrEngagement() = transferManager.startQrEngagement()

    /**
     * Starts the transfer process by engaging with the reader via appLink
     *
     * @see [TransferManager.startEngagementToApp]
     * @param intent
     * @throws IllegalStateException if [EudiWallet] is not firstly initialized via the [init] method
     */
    fun startEngagementToApp(intent: Intent) = transferManager.startEngagementToApp(intent)

    /**
     * Creates a response for the given [disclosedDocuments] that will be sent to the reader
     *
     * @param disclosedDocuments
     * @return
     */
    fun createResponse(disclosedDocuments: DisclosedDocuments): ResponseResult =
        transferManager.createResponse(disclosedDocuments)

    /**
     * Sends the given [responseBytes] to the reader
     *
     * @param responseBytes
     */
    fun sendResponse(responseBytes: ByteArray) = transferManager.sendResponse(responseBytes)

    /**
     * Stops the transfer process
     *
     * @param sendSessionTerminationMessage
     * @param useTransportSpecificSessionTermination
     */
    fun stopPresentation(
        sendSessionTerminationMessage: Boolean = true,
        useTransportSpecificSessionTermination: Boolean = false,
    ) = transferManager.stopPresentation(
        sendSessionTerminationMessage,
        useTransportSpecificSessionTermination
    )

    private fun <T> requireInit(block: () -> T): T {
        if (!::context.isInitialized) {
            throw IllegalStateException("EudiWallet.init() must be called before using the SDK")
        }
        return block()
    }

    private val deviceRetrievalMethods
        get() = listOf(
            BleRetrievalMethod(
                peripheralServerMode = config.blePeripheralServerModeEnabled,
                centralClientMode = config.bleCentralClientModeEnabled,
                clearBleCache = config.bleClearCacheEnabled,
            )
        )

    private val transferManagerDocumentsResolver: DocumentsResolver
        get() = DocumentsResolver { req ->
            documentManager.getDocuments().filter { doc ->
                doc.docType == req.docType
            }.map { doc ->
                RequestDocument(
                    documentId = doc.id,
                    docType = doc.docType,
                    docName = doc.name,
                    userAuthentication = doc.requiresUserAuth,
                    docRequest = req
                )
            }
        }

}