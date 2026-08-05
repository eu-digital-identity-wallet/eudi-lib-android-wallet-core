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

@file:JvmMultifileClass
package eu.europa.ec.eudi.wallet.dcapi

import eu.europa.ec.eudi.wallet.dcapi.internal.*
import android.content.Intent
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.provider.PendingIntentHandler
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.i
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor

/**
 * Manages requests and responses for the Digital Credential API (DCAPI).
 *
 * Each incoming request is routed to the processor for its protocol: ISO mdoc requests
 * (ISO/IEC TS 18013-7:2025 Annex C) to [isoMdocRequestProcessor], and OpenID4VP requests to
 * [openId4VpRequestProcessor] when it is configured. A request for a protocol that is not in
 * [supportedProtocols] is rejected.
 *
 * @property isoMdocRequestProcessor the processor for ISO mdoc requests.
 * @property openId4VpRequestProcessor the processor for OpenID4VP requests, or null when OpenID4VP
 *   over the DCAPI is not configured.
 * @property supportedProtocols the protocols this manager will process.
 * @property logger optional logger for logging events.
 * @property listenersExecutor optional executor for running listener callbacks.
 */

class DCAPIManager(
    private val isoMdocRequestProcessor: RequestProcessor,
    private val openId4VpRequestProcessor: RequestProcessor? = null,
    private val supportedProtocols: List<DCAPIProtocol>,
    var logger: Logger? = null,
    var listenersExecutor: Executor? = null,
) : TransferEvent.Listenable, ReaderTrustStoreAware {

    override var readerTrustStore: ReaderTrustStore?
        get() = (isoMdocRequestProcessor as? ReaderTrustStoreAware)?.readerTrustStore
            ?: (openId4VpRequestProcessor as? ReaderTrustStoreAware)?.readerTrustStore
        set(value) {
            (isoMdocRequestProcessor as? ReaderTrustStoreAware)?.readerTrustStore = value
            (openId4VpRequestProcessor as? ReaderTrustStoreAware)?.readerTrustStore = value
        }

    private val transferEventListeners: MutableList<TransferEvent.Listener> = mutableListOf()

    override fun addTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferEventListeners.add(listener)
    }

    override fun removeTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferEventListeners.remove(listener)
    }

    override fun removeAllTransferEventListeners() = apply {
        transferEventListeners.clear()
    }

    fun resolveRequest(request: Request) {
        require(request is DCAPIRequest) { "Request must be an DCAPIRequest" }
        logger?.d(TAG, "Resolving DCAPI request")
        logger?.d(TAG, "DC API request JSON: ${request.providerGetCredentialRequest.requestJsonOrNull()}")
        val protocol = try {
            request.providerGetCredentialRequest.resolveDcApiRequest(supportedProtocols).protocol
        } catch (e: Exception) {
            emitError(e.message ?: "No supported DC API protocol found for this request", e)
            return
        }
        logger?.i(TAG, "DC API request received (protocol=$protocol)")
        when (protocol) {
            DCAPIProtocol.ISO_MDOC.identifier ->
                processWith(isoMdocRequestProcessor, request, protocol)

            DCAPIProtocol.OPENID4VP_V1_UNSIGNED.identifier,
            DCAPIProtocol.OPENID4VP_V1_SIGNED.identifier -> {
                val processor = openId4VpRequestProcessor
                if (processor == null) {
                    emitError("OpenID4VP over DC API is not configured (missing openId4VpConfig)")
                } else {
                    processWith(processor, request, protocol)
                }
            }

            else -> emitError("Unsupported protocol: $protocol")
        }
    }

    private fun processWith(processor: RequestProcessor, request: DCAPIRequest, protocol: String) {
        try {
            logger?.d(TAG, "Processing request for protocol: $protocol")
            val processedRequest = runBlocking { processor.process(request) }
            transferEventListeners.onTransferEvent(
                TransferEvent.RequestReceived(processedRequest = processedRequest, request = request)
            )
            logger?.i(TAG, "DC API request processed (protocol=$protocol); awaiting user consent")
        } catch (e: Exception) {
            logger?.e(TAG, "Error processing request for protocol: $protocol", e)
            transferEventListeners.onTransferEvent(
                TransferEvent.Error(
                    DCAPIException("Error processing request for protocol: $protocol", e)
                )
            )
        }
    }

    private fun emitError(message: String, cause: Throwable? = null) {
        logger?.e(TAG, message, cause)
        transferEventListeners.onTransferEvent(TransferEvent.Error(DCAPIException(message, cause)))
    }

    fun sendResponse(response: Response) {
        require(response is DCAPIResponse) { "Response must be an DCAPIResponse" }
        logger?.i(TAG, "Sending DC API response")
        transferEventListeners.onTransferEvent(TransferEvent.IntentToSend(response.intent))
    }

    private fun List<TransferEvent.Listener>.onTransferEvent(
        event: TransferEvent,
    ) {
        val executor = listenersExecutor ?: Dispatchers.Main.asExecutor()
        transferEventListeners.forEach { executor.execute { it.onTransferEvent(event) } }
    }

    companion object {
        private const val TAG = "DCAPIManager"
    }
}

class DCAPIException(message: String, cause: Throwable? = null): Exception(message, cause) {
    fun toIntent(): Intent {
        val resultData = Intent()
        PendingIntentHandler.setGetCredentialException(
            resultData,
            GetCredentialCustomException(
                type = cause?.toString() ?: "Unknown Error Type",
                errorMessage = message
            )
        )
        return resultData
    }
}