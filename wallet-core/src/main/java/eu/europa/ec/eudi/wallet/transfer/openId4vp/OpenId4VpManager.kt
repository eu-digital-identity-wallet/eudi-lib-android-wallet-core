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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import android.net.Uri
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.openid4vp.DefaultHttpClientFactory
import eu.europa.ec.eudi.openid4vp.DispatchOutcome
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.i
import eu.europa.ec.eudi.wallet.internal.toSiopOpenId4VPConfig
import eu.europa.ec.eudi.wallet.internal.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.internal.wrappedWithLogging
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.util.CBOR.cborPrettyPrint
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.bouncycastle.util.encoders.Hex.toHexString
import java.util.concurrent.Executor

class OpenId4VpManager(
    val config: OpenId4VpConfig,
    val requestProcessor: OpenId4VpRequestProcessor,
    var logger: Logger? = null,
    var listenersExecutor: Executor? = null,
    val ktorHttpClientFactory: (() -> HttpClient)? = null,
) : TransferEvent.Listenable, ReaderTrustStoreAware {

    override var readerTrustStore: ReaderTrustStore?
        get() = requestProcessor.readerTrustStore
        set(value) {
            requestProcessor.readerTrustStore = value
        }


    private val siopOpenId4Vp by lazy {
        SiopOpenId4Vp(
            siopOpenId4VPConfig = config.toSiopOpenId4VPConfig(
                trust = requestProcessor.openid4VpX509CertificateTrustStore
            ),
            httpClientFactory = (ktorHttpClientFactory ?: DefaultHttpClientFactory)
                .wrappedWithLogging(logger)
                .wrappedWithContentNegotiation()
        )
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        transferEventListeners.onTransferEvent(TransferEvent.Error(e))
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    private var resolveRequestUriJob: Job? = null
    private var sendResponseJob: Job? = null

    /**
     * Resolves a request URI. This method is asynchronous and the result is emitted through the
     * [TransferEvent.Listener] interface. Every time it is called it cancels any previous request
     * that is being resolved. This will lead to the [TransferEvent.Disconnected] event being emitted.
     * @see eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpManager.addTransferEventListener
     *
     */
    fun resolveRequestUri(uri: String) {
        resolveRequestUriJob?.cancel()
        resolveRequestUriJob = scope.launch {
            try {
                require(config.schemes.contains(Uri.parse(uri).scheme)) {
                    "Not supported scheme for OpenId4Vp"
                }
                when (val resolution = siopOpenId4Vp.resolveRequestUri(uri)) {
                    is Resolution.Invalid -> {

                        // TODO dispatch error to verifier/RP
                        //  if resolution.dispatchDetails is present

                        logger?.e(TAG, "Invalid resolution: ${resolution.error}")
                        transferEventListeners.onTransferEvent(
                            TransferEvent.Error(
                                resolution.error.asException()
                            )
                        )
                    }

                    is Resolution.Success -> {
                        logger?.d(TAG, "Resolution.Success")
                        when (val resolvedRequest = resolution.requestObject) {
                            is ResolvedRequestObject.OpenId4VPAuthorization -> {
                                logger?.i(TAG, "${resolvedRequest::class.simpleName} received")
                                val request = OpenId4VpRequest(resolvedRequest)
                                val processedRequest = requestProcessor.process(request)
                                transferEventListeners.onTransferEvent(
                                    TransferEvent.RequestReceived(processedRequest, request)
                                )
                            }

                            is ResolvedRequestObject.SiopAuthentication,
                            is ResolvedRequestObject.SiopOpenId4VPAuthentication,
                                -> {
                                logger?.i(TAG, "${resolvedRequest::class.simpleName} received")
                                transferEventListeners.onTransferEvent(
                                    TransferEvent.Error(
                                        IllegalArgumentException("Unsupported request type")
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                logger?.e(TAG, "Failed to resolve request URL", e)
                val event = when (e) {
                    is CancellationException -> TransferEvent.Disconnected
                    else -> TransferEvent.Error(e)
                }
                transferEventListeners.onTransferEvent(event)
            }
        }
    }

    /**
     * Sends a response to the verifier.
     * This method is asynchronous and the result is emitted through the [TransferEvent.Listener]
     * interface.
     * Every time it is called it cancels any previous response that is being sent. This will lead
     * to the [TransferEvent.Disconnected] event being emitted.
     * @see eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpManager.addTransferEventListener
     *
     * @param response The response to send.
     */
    fun sendResponse(response: Response) {
        sendResponseJob?.cancel()
        sendResponseJob = scope.launch {
            try {
                require(response is OpenId4VpResponse) {
                    "Response must be an OpenId4VpResponse"
                }
                require(response.resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization) {
                    "Resolved request object must be OpenId4VPAuthorization"
                }

                logResponse(response)


                when (val outcome = siopOpenId4Vp.dispatch(
                    request = response.resolvedRequestObject,
                    consensus = response.consensus,
                    encryptionParameters = response.encryptionParameters,
                )) {
                    is DispatchOutcome.RedirectURI -> {
                        logger?.d(TAG, "Verifier respond with RedirectURI: ${outcome.value}")
                        transferEventListeners.onTransferEvent(TransferEvent.ResponseSent)
                    }

                    is DispatchOutcome.VerifierResponse.Accepted -> {
                        logger?.d(TAG, "Verifier accepted the response")
                        when (val uri = outcome.redirectURI) {
                            null -> transferEventListeners.onTransferEvent(TransferEvent.ResponseSent)
                            else -> {
                                logger?.d(TAG, "Redirecting to: $uri")
                                transferEventListeners.onTransferEvent(TransferEvent.Redirect(uri))
                            }
                        }
                    }

                    DispatchOutcome.VerifierResponse.Rejected -> {
                        logger?.e(TAG, "Verifier rejected the response")
                        transferEventListeners.onTransferEvent(
                            TransferEvent.Error(
                                IllegalStateException("Verifier rejected the response")
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                logger?.e(TAG, "Failed to send response", e)
                val event = when (e) {
                    is CancellationException -> TransferEvent.Disconnected
                    else -> TransferEvent.Error(e)
                }
                transferEventListeners.onTransferEvent(event)
            }
        }
    }

    /**
     * Stops the manager and cancels all running connections made by the manager.
     * When a connection is cancelled, the [TransferEvent.Disconnected] event is emitted.
     */
    fun stop() {
        logger?.d(TAG, "Stopping OpenId4VpManager")
        scope.coroutineContext.cancelChildren()
    }

    private fun logResponse(response: OpenId4VpResponse) {
        when (response) {
            is OpenId4VpResponse.DeviceResponse -> {
                logger?.d(
                    TAG,
                    "Device Response to send (hex): ${toHexString(response.responseBytes)}"
                )
                logger?.d(
                    TAG,
                    "Device Response to send (cbor): ${cborPrettyPrint(response.responseBytes)}"
                )
                logger?.d(TAG, "VpContent: ${response.vpContent}")
            }

            is OpenId4VpResponse.GenericResponse -> {
                logger?.d(
                    TAG,
                    "Generic Response to send: ${response.response.joinToString("\n")}"
                )
                logger?.d(TAG, "VpContent: ${response.vpContent}")
            }
        }
    }

    private fun List<TransferEvent.Listener>.onTransferEvent(
        event: TransferEvent,
    ) {
        val executor = listenersExecutor ?: Dispatchers.Main.asExecutor()
        transferEventListeners.forEach { executor.execute { it.onTransferEvent(event) } }
    }

    companion object {
        private const val TAG = "OpenId4VpManager"
    }
}