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

import android.content.Intent
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.google.android.gms.identitycredentials.IntentHelper
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.json.JSONObject
import java.util.concurrent.Executor

/**
 * [DCAPIManager] is responsible for managing requests and responses for the Digital Credential API (DCAPI).
 * Currently, it supports the protocol `org-iso-mdoc` according to the ISO/IEC TS 18013-7:2025 Annex C.
 *
 * @property requestProcessor The processor that handles the requests.
 * @property logger Optional logger for logging events.
 * @property listenersExecutor Optional executor for running listener callbacks.
 */

class DCAPIManager(
    private val requestProcessor: RequestProcessor,
    var logger: Logger? = null,
    var listenersExecutor: Executor? = null,
) : TransferEvent.Listenable, ReaderTrustStoreAware {

    override var readerTrustStore: ReaderTrustStore?
        get() = if (requestProcessor is ReaderTrustStoreAware) requestProcessor.readerTrustStore else null
        set(value) {
            if (requestProcessor is ReaderTrustStoreAware) requestProcessor.readerTrustStore = value
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
        when (val protocol = request.providerGetCredentialRequest.getProtocol()) {
            DC_API_PROTOCOL_ORG_ISO_MDOC -> {
                try {
                    logger?.d(TAG, "Processing request for protocol: $protocol")
                    val processedRequest = requestProcessor.process(request)
                    transferEventListeners.onTransferEvent(
                        TransferEvent.RequestReceived(
                            processedRequest = processedRequest,
                            request = request
                        )
                    )
                } catch (e: Exception) {
                    logger?.e(TAG, "Error processing request for protocol: $protocol", e)
                    transferEventListeners.onTransferEvent(
                        TransferEvent.Error(
                            DCAPIException("Error processing request for protocol: $protocol", e)
                        )
                    )
                }
            }

            else -> {
                logger?.e(TAG, "Unsupported protocol: $protocol")
                transferEventListeners.onTransferEvent(
                    TransferEvent.Error(
                        DCAPIException("Unsupported protocol: $protocol")
                    )
                )
            }
        }
    }

    fun sendResponse(response: Response) {
        require(response is DCAPIResponse) { "Response must be an DCAPIResponse" }
        transferEventListeners.onTransferEvent(TransferEvent.IntentToSend(response.intent))
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun ProviderGetCredentialRequest.getProtocol(): String {
        val option = this.credentialOptions[0] as GetDigitalCredentialOption
        val requestJson = JSONObject(option.requestJson)
        val firstRequest = requestJson.getJSONArray(REQUESTS).getJSONObject(0)
        return firstRequest.getString(PROTOCOL)
    }

    private fun List<TransferEvent.Listener>.onTransferEvent(
        event: TransferEvent,
    ) {
        val executor = listenersExecutor ?: Dispatchers.Main.asExecutor()
        transferEventListeners.forEach { executor.execute { it.onTransferEvent(event) } }
    }

    companion object {
        private const val TAG = "DCAPIManager"
        private const val DC_API_PROTOCOL_ORG_ISO_MDOC = "org-iso-mdoc"
    }
}

class DCAPIException(message: String, cause: Throwable? = null): Exception(message, cause) {
    fun toIntent(): Intent {
        val resultData = Intent()
        IntentHelper.setGetCredentialException(
            resultData,
            cause?.toString() ?: "Unknown Error Type",
            message
        )
        return resultData
    }
}