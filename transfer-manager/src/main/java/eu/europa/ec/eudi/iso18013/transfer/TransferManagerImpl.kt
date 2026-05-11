/*
 * Copyright (c) 2024-2026 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import eu.europa.ec.eudi.iso18013.transfer.engagement.DeviceRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.iso18013.transfer.internal.QrEngagement
import eu.europa.ec.eudi.iso18013.transfer.internal.TAG
import eu.europa.ec.eudi.iso18013.transfer.internal.stopPresentation
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import eu.europa.ec.eudi.wallet.document.DocumentManager
import kotlinx.coroutines.runBlocking
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.util.Constants

/**
 * Transfer Manager class used for performing device engagement and data retrieval
 * for ISO 18013-5 standard.
 *
 * @property retrievalMethods list of device retrieval methods to be used for the transfer
 *
 * @constructor Create a Transfer Manager
 * @param context application context
 * @param requestProcessor request processor for processing the device request and generating the response
 * @param retrievalMethods list of device retrieval methods to be used for the transfer
 */
class TransferManagerImpl @JvmOverloads constructor(
    context: Context,
    override val requestProcessor: RequestProcessor,
    retrievalMethods: List<DeviceRetrievalMethod>? = null,
) : TransferManager, ReaderTrustStoreAware {
    private val context = context.applicationContext

    override var readerTrustStore: ReaderTrustStore?
        get() = (requestProcessor as? ReaderTrustStoreAware)?.readerTrustStore
        set(value) {
            (requestProcessor as? ReaderTrustStoreAware)?.readerTrustStore = value
        }

    /**
     * Device retrieval helper instance
     */
    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var deviceRetrievalHelper: DeviceRetrievalHelper? = null

    /**
     * QR engagement instance
     */
    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var qrEngagement: QrEngagement? = null

    /**
     * Flag to check if the transfer has started
     */
    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var hasStarted = false

    /**
     * List of device retrieval methods
     */
    var retrievalMethods: List<DeviceRetrievalMethod> =
        retrievalMethods?.let { listOf(*it.toTypedArray()) } ?: emptyList()
        private set(value) {
            field = listOf(*value.toTypedArray())
        }

    /**
     * List of transfer event listeners
     */
    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val transferEventListeners = mutableListOf<TransferEvent.Listener>()

    /**
     * Add a transfer event listener
     * @param listener a transfer event listener
     * @return instance of [TransferManager]
     */
    override fun addTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferEventListeners.add(listener)
    }

    /**
     * Remove a transfer event listener
     * @param listener a transfer event listener
     * @return instance of [TransferManager]
     */
    override fun removeTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferEventListeners.remove(listener)
    }

    /**
     * Remove all transfer event listeners
     * @return instance of [TransferManager]
     */
    override fun removeAllTransferEventListeners() = apply {
        transferEventListeners.clear()
    }

    /**
     * Set retrieval methods
     * @param retrievalMethods list of device retrieval methods
     * @return instance of [TransferManager]
     */
    override fun setRetrievalMethods(retrievalMethods: List<DeviceRetrievalMethod>) = apply {
        this.retrievalMethods = retrievalMethods
    }

    /**
     * Starts the QR Engagement and generates the QR code
     * Once the QR code is ready, the event [TransferEvent.QrEngagementReady] will be triggered
     * If the transfer has already started, an error event will be triggered
     * with an [IllegalStateException] containing the message "Transfer has already started."
     * @see TransferEvent.QrEngagementReady
     * @see TransferEvent.Error
     */
    override fun startQrEngagement() {
        if (hasStarted) {
            Log.d(this.TAG, TRANSFER_STARTED_MSG)
            transferEventListeners.onTransferEvent(
                TransferEvent.Error(
                    IllegalStateException(
                        TRANSFER_STARTED_MSG,
                    ),
                ),
            )
            return
        }
        qrEngagement = QrEngagement(
            context = context,
            retrievalMethods = retrievalMethods,
            onQrEngagementReady = { qrCode ->
                transferEventListeners.onTransferEvent(
                    TransferEvent.QrEngagementReady(qrCode),
                )
            },
            onConnecting = {
                transferEventListeners.onTransferEvent(TransferEvent.Connecting)
            },
            onDeviceRetrievalHelperReady = { deviceRetrievalHelper ->
                this.deviceRetrievalHelper = deviceRetrievalHelper
                transferEventListeners.onTransferEvent(TransferEvent.Connected)
            },
            onNewDeviceRequest = { deviceRequestBytes ->
                val deviceRequest = DeviceRequest(
                    deviceRequestBytes,
                    deviceRetrievalHelper?.sessionTranscript!!
                )
                transferEventListeners.onTransferEvent(
                    TransferEvent.RequestReceived(
                        processedRequest = runBlocking {
                            requestProcessor.process(deviceRequest)
                        },
                        request = deviceRequest
                    )
                )
            },
            onDisconnected = {
                transferEventListeners.onTransferEvent(TransferEvent.Disconnected)
            },
            onCommunicationError = { error ->
                Log.d(this.TAG, "onError: ${error.message}")
                transferEventListeners.onTransferEvent(TransferEvent.Error(error))
            },
        ).apply { configure() }
        hasStarted = true
    }

    /**
     * Sets up NFC engagement with the provided service
     * Note: This method is only for internal use and should not be called by the app
     * @param service the NFC engagement service
     * @return instance of [TransferManager]
     */
    override fun setupNfcEngagement(service: NfcEngagementService) = apply {
        service.apply {
            retrievalMethods = this@TransferManagerImpl.retrievalMethods
            onConnecting = {
                transferEventListeners.onTransferEvent(TransferEvent.Connecting)
            }
            onDeviceRetrievalHelperReady = { deviceRetrievalHelper ->
                this@TransferManagerImpl.deviceRetrievalHelper = deviceRetrievalHelper
                transferEventListeners.onTransferEvent(TransferEvent.Connected)
            }
            onNewDeviceRequest = { deviceRequestBytes ->
                val deviceRequest = DeviceRequest(
                    deviceRequestBytes,
                    deviceRetrievalHelper?.sessionTranscript!!
                )
                transferEventListeners.onTransferEvent(
                    TransferEvent.RequestReceived(
                        processedRequest = runBlocking {
                            requestProcessor.process(deviceRequest)
                        },
                        request = deviceRequest
                    )
                )
            }
            onDisconnected = {
                transferEventListeners.onTransferEvent(TransferEvent.Disconnected)
            }
            onCommunicationError = { error ->
                Log.d(this.TAG, "onError: ${error.message}")
                transferEventListeners.onTransferEvent(TransferEvent.Error(error))
            }
        }
    }

    /**
     * Sends the response bytes to the connected mdoc verifier and terminates the session.
     *
     * **Note:** Currently, only a single request-response cycle per session is supported.
     * The response is sent with [Constants.SESSION_DATA_STATUS_SESSION_TERMINATION], which
     * signals the end of the session to the verifier. Although ISO 18013-5 permits multiple
     * request-response exchanges within a single session, this library always terminates
     * the session after the first response.
     *
     * To generate the response, use the [eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest.generateResponse]
     * that is provided by the [eu.europa.ec.eudi.iso18013.transfer.TransferEvent.RequestReceived] event.
     * @param response the response to send
     * @throws IllegalArgumentException if the response is not a [DeviceResponse]
     */
    override fun sendResponse(response: Response) {
        require(response is DeviceResponse) { "Response must be a DeviceResponse" }
        deviceRetrievalHelper?.sendDeviceResponse(
            response.deviceResponseBytes,
            Constants.SESSION_DATA_STATUS_SESSION_TERMINATION
        )
        transferEventListeners.onTransferEvent(TransferEvent.ResponseSent)
    }

    /**
     * Closes the connection and clears the data of the session
     * Also, sends a termination message if there is a connected mdoc verifier
     *
     * @param sendSessionTerminationMessage Whether to send session termination message.
     * @param useTransportSpecificSessionTermination Whether to use transport-specific session
     */
    override fun stopPresentation(
        sendSessionTerminationMessage: Boolean,
        useTransportSpecificSessionTermination: Boolean,
    ) {
        deviceRetrievalHelper?.stopPresentation(
            sendSessionTerminationMessage,
            useTransportSpecificSessionTermination,
        )
        disconnect()
    }

    /**
     * Disconnects the current session and clears the engagement objects
     */
    private fun disconnect() {
        qrEngagement?.close()
        destroy()
    }

    /**
     * Destroys the current session and clears the engagement objects
     */
    private fun destroy() {
        deviceRetrievalHelper = null
        qrEngagement = null
        hasStarted = false
    }

    /**
     * Companion object for creating a new instance of [TransferManager]
     */
    companion object {
        private const val TRANSFER_STARTED_MSG = "Transfer has already started."

        /**
         * Extension function to trigger transfer events for each listener in the collection
         *
         * @param event the transfer event to be triggered
         */
        private fun Collection<TransferEvent.Listener>.onTransferEvent(event: TransferEvent) {
            forEach { it.onTransferEvent(event) }
        }

        /**
         * Create a new instance of [TransferManager]
         */
        operator fun invoke(context: Context, builder: Builder.() -> Unit): TransferManagerImpl {
            val builder = Builder(context).apply(builder)
            return builder.build()
        }
    }

    /**
     * Builder class for instantiating a [TransferManager] implementation
     *
     * @property documentManager document manager instance
     * @property readerTrustStore reader trust store instance
     * @property retrievalMethods list of device retrieval methods
     * @property zkSystemRepository ZK system repository instance
     * @property zkResponsePolicy ZK response policy
     * @constructor
     * @param context
     */
    class Builder(context: Context) {
        private val context = context.applicationContext
        var documentManager: DocumentManager? = null
        var readerTrustStore: ReaderTrustStore? = null
        var readerAuthPolicy: ReaderAuthPolicy = ReaderAuthPolicy.EnforceIfPresent
        var retrievalMethods: List<DeviceRetrievalMethod>? = null
        var zkSystemRepository: ZkSystemRepository? = null
        var zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.FallbackToFullDisclosure

        /**
         * Document manager instance that will be used to retrieve the requested documents
         * @param documentManager
         */
        fun documentManager(documentManager: DocumentManager) = apply {
            this.documentManager = documentManager
        }

        /**
         * Reader trust store instance that will be used to verify the reader's certificate
         * @param readerTrustStore
         */
        fun readerTrustStore(readerTrustStore: ReaderTrustStore) = apply {
            this.readerTrustStore = readerTrustStore
        }

        /**
         * Policy for enforcing reader authentication results during response generation.
         * Default is [ReaderAuthPolicy.EnforceIfPresent].
         * @param readerAuthPolicy the reader authentication policy
         */
        fun readerAuthPolicy(readerAuthPolicy: ReaderAuthPolicy) = apply {
            this.readerAuthPolicy = readerAuthPolicy
        }

        /**
         * Retrieval methods that will be used to retrieve the device request from the mdoc verifier
         * @param retrievalMethods
         */
        fun retrievalMethods(retrievalMethods: List<DeviceRetrievalMethod>) =
            apply { this.retrievalMethods = retrievalMethods }

        /**
         * ZK system repository that holds the zero-knowledge proof systems
         * @param zkSystemRepository
         */
        fun zkSystemRepository(zkSystemRepository: ZkSystemRepository) = apply {
            this.zkSystemRepository = zkSystemRepository
        }

        /**
         * ZK response policy that determines behavior when ZK proof generation fails.
         * Defaults to [ZkResponsePolicy.FallbackToFullDisclosure] for backwards compatibility.
         * Consider using [ZkResponsePolicy.Strict] for production to prevent unintended full disclosure.
         * @param zkResponsePolicy
         */
        fun zkResponsePolicy(zkResponsePolicy: ZkResponsePolicy) = apply {
            this.zkResponsePolicy = zkResponsePolicy
        }

        /**
         * Build a [eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl] instance
         * with [DeviceRequestProcessor] instance
         * @return [eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl]
         */
        fun build(): TransferManagerImpl {
            requireNotNull(documentManager) { "Document manager must be provided" }
            return TransferManagerImpl(
                context = context,
                requestProcessor = DeviceRequestProcessor(
                    documentManager = documentManager!!,
                    readerTrustStore = readerTrustStore,
                    readerAuthPolicy = readerAuthPolicy,
                    zkSystemRepository = zkSystemRepository,
                    zkResponsePolicy = zkResponsePolicy,
                ),
                retrievalMethods = retrievalMethods ?: emptyList()
            )
        }
    }
}