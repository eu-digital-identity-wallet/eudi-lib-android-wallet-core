/*
 * Copyright (c) 2023-2026 European Commission
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
import eu.europa.ec.eudi.iso18013.transfer.engagement.DeviceRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import eu.europa.ec.eudi.wallet.document.DocumentManager
import org.multipaz.mdoc.zkp.ZkSystemRepository

/**
 * Transfer manager interface for managing the transfer of data between the wallet and the reader.
 */
interface TransferManager : TransferEvent.Listenable {

    val requestProcessor: RequestProcessor

    /**
     * Set retrieval methods
     *
     * @param retrievalMethods
     * @return a [TransferManager]
     */
    fun setRetrievalMethods(retrievalMethods: List<DeviceRetrievalMethod>): TransferManager

    /**
     * Setup the [NfcEngagementService]
     * Note: This method is only for internal use and should not be called by the app
     * @param service
     * @see NfcEngagementService
     */
    fun setupNfcEngagement(service: NfcEngagementService): TransferManager

    /**
     * Starts the QR Engagement and generates the QR code
     * Once the QR code is ready, the event [TransferEvent.QrEngagementReady] will be triggered
     */
    fun startQrEngagement()

    /**
     * Sends response bytes to the connected reader and terminates the session.
     *
     * **Note:** Each session supports a single request-response cycle.
     * Sending a response terminates the presentation session;
     * start a new session to perform another exchange. This is conformant
     * with ISO/IEC 18013-5:2021 §9.1.1.4, which makes additional
     * exchanges optional, not required.
     *
     * To generate the response, use the [RequestProcessor.ProcessedRequest.Success.generateResponse]
     * method.
     * @param response The response to be sent
     */
    fun sendResponse(response: Response)

    /**
     * Closes the connection and clears the data of the session
     * Also, sends a termination message if there is a connected verifier
     *
     * @param sendSessionTerminationMessage Whether to send session termination message.
     * @param useTransportSpecificSessionTermination Whether to use transport-specific session
     */
    fun stopPresentation(
        sendSessionTerminationMessage: Boolean = true,
        useTransportSpecificSessionTermination: Boolean = false,
    )

    /**
     * Companion object for creating a new instance of [TransferManager]
     */
    companion object {
        /**
         * Create a new instance of [TransferManager] for the ISO 18013-5
         * standard.
         *
         * @param context
         * @param documentManager
         * @param readerTrustStore
         * @param readerAuthPolicy
         * @param retrievalMethods
         * @param zkSystemRepository
         * @param zkResponsePolicy the ZK response policy to use when ZK proof generation fails.
         * Defaults to [ZkResponsePolicy.Strict] to prevent unintended full disclosure.
         * @return a [TransferManagerImpl]
         */
        @JvmStatic
        fun getDefault(
            context: Context,
            documentManager: DocumentManager,
            readerTrustStore: ReaderTrustStore? = null,
            readerAuthPolicy: ReaderAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
            retrievalMethods: List<DeviceRetrievalMethod>? = null,
            zkSystemRepository: ZkSystemRepository? = null,
            zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.Strict
        ): TransferManager = TransferManagerImpl(context) {
            documentManager(documentManager)
            readerTrustStore?.let { readerTrustStore(it) }
            readerAuthPolicy(readerAuthPolicy)
            retrievalMethods?.let { retrievalMethods(it) }
            zkSystemRepository?.let { zkSystemRepository(it) }
            zkResponsePolicy(zkResponsePolicy)
        }
    }
}
