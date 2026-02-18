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
import com.nimbusds.jose.util.Base64URL
import org.jetbrains.annotations.VisibleForTesting
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.openid4vp.AuthorizationRequestError
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.DispatchOutcome
import eu.europa.ec.eudi.openid4vp.EncryptionParameters
import eu.europa.ec.eudi.openid4vp.ErrorDispatchDetails
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.generateJarmNonce
import eu.europa.ec.eudi.wallet.internal.i
import eu.europa.ec.eudi.wallet.internal.makeOpenId4VPConfig
import eu.europa.ec.eudi.wallet.internal.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.internal.wrappedWithLogging
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Manages the OpenID4VP (OpenID for Verifiable Presentations) flow in the wallet.
 *
 * This class is responsible for configuring, initializing, and orchestrating the OpenID4VP
 * protocol, including request processing, event listening, and HTTP client management.
 * It acts as the main entry point for handling OpenID4VP requests and responses in the wallet.
 *
 * @property config The OpenID4VP configuration for the wallet.
 * @property requestProcessor The dispatcher that routes requests to the appropriate processor.
 * @property logger Optional logger for diagnostic output.
 * @property listenersExecutor Optional executor for event listeners.
 * @property ktorHttpClientFactory Optional factory for creating custom Ktor HTTP clients.
 */
class OpenId4VpManager(
    val config: OpenId4VpConfig,
    val requestProcessor: DcqlRequestProcessor,
    var logger: Logger? = null,
    var listenersExecutor: Executor? = null,
    val ktorHttpClientFactory: (() -> HttpClient)? = null
) : TransferEvent.Listenable, ReaderTrustStoreAware {

    /**
     * The trust store used for verifying reader certificates. Delegates to the request processor.
     */
    override var readerTrustStore: ReaderTrustStore?
        get() = requestProcessor.readerTrustStore
        set(value) {
            requestProcessor.readerTrustStore = value
        }

    /**
     * Lazy initialization of the OpenID4VP protocol handler with logging and content negotiation.
     * Uses the configuration and trust anchor from the request processor.
     */
    private val openId4Vp by lazy {
        OpenId4Vp(
            openId4VPConfig = makeOpenId4VPConfig(
                config,
                requestProcessor.openid4VpX509CertificateTrust
            ),
            httpClient = (ktorHttpClientFactory ?: DefaultHttpClientFactory)
                .wrappedWithLogging(logger)
                .wrappedWithContentNegotiation()
                .invoke()
        )
    }

    /**
     * Caches the currently active [ResolvedRequestObject] during a remote presentation flow.
     *
     * This object is populated upon successful resolution of a request URI and is cleared
     * when the presentation is stopped or rejected. It is essential for supporting
     * user-initiated rejection ([reject]), as it allows the SDK to construct the correct
     * negative consensus response without requiring the UI to pass the request object back.
     */
    private var activeRequestObject: ResolvedRequestObject? = null

    /**
     * List of listeners for transfer events (request received, response sent, errors, etc).
     */
    private val transferEventListeners: MutableList<TransferEvent.Listener> = mutableListOf()

    /**
     * Registers a new transfer event listener.
     */
    override fun addTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferEventListeners.add(listener)
    }

    /**
     * Removes a transfer event listener.
     */
    override fun removeTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferEventListeners.remove(listener)
    }

    /**
     * Removes all transfer event listeners.
     */
    override fun removeAllTransferEventListeners() = apply {
        transferEventListeners.clear()
    }

    /**
     * Handles uncaught exceptions in coroutines by notifying all transfer event listeners.
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        transferEventListeners.onTransferEvent(TransferEvent.Error(e))
    }

    /**
     * Coroutine scope for running asynchronous operations (request resolution, response sending).
     * Uses IO dispatcher, supervisor job, and the exception handler above.
     */
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
        activeRequestObject = null
        resolveRequestUriJob?.cancel()
        resolveRequestUriJob = scope.launch {
            try {
                require(config.schemes.contains(Uri.parse(uri).scheme)) {
                    "Not supported scheme for OpenId4Vp"
                }
                when (val resolution = openId4Vp.resolveRequestUri(uri)) {
                    is Resolution.Invalid -> {

                        val error = resolution.error
                        logger?.e(TAG, "Invalid resolution: $error")

                        val details = resolution.dispatchDetails
                        if (details != null) {
                            dispatchErrorResponse(error, details)
                        }

                        transferEventListeners.onTransferEvent(
                            TransferEvent.Error(error.asException())
                        )
                    }

                    is Resolution.Success -> {
                        logger?.d(TAG, "Resolution.Success")
                        val resolvedRequest = resolution.requestObject
                        activeRequestObject = resolvedRequest
                        logger?.i(TAG, "${resolvedRequest::class.simpleName} received")
                        val request = OpenId4VpRequest(resolvedRequest)
                        val processedRequest = requestProcessor.process(request)
                        transferEventListeners.onTransferEvent(
                            TransferEvent.RequestReceived(processedRequest, request)
                        )
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
     * Dispatches a protocol-level error to the Verifier.
     *
     * This method is used when the Authorization Request cannot be fully resolved or validated
     * (e.g., unsupported scheme, missing scope, or invalid format), but enough information
     * (the [dispatchDetails]) was parsed to allow for a proper error response.
     *
     * Sending this error ensures the Verifier is notified of the failure instead of waiting
     * indefinitely for a response.
     *
     * @param error The specific [AuthorizationRequestError] describing why the request failed.
     * @param dispatchDetails The details (redirect URI, state, nonce) required to send the error.
     */
    private suspend fun dispatchErrorResponse(
        error: AuthorizationRequestError,
        dispatchDetails: ErrorDispatchDetails
    ) {
        try {
            logger?.d(TAG, "Dispatching error: $error via ${dispatchDetails.responseMode}")

            val encryptionSpec = dispatchDetails.responseEncryptionSpecification

            val encParams = if (encryptionSpec != null) {
                val randomApu = generateJarmNonce()
                EncryptionParameters.DiffieHellman(apu = Base64URL.encode(randomApu))
            } else {
                null
            }

            val outcome = openId4Vp.dispatchError(
                error = error,
                errorDispatchDetails = dispatchDetails,
                encryptionParameters = encParams
            )

            if (outcome is DispatchOutcome.VerifierResponse.Accepted) {
                logger?.d(TAG, "Error successfully dispatched to Verifier")
            }
        } catch (e: Exception) {
            logger?.e(TAG, "Failed to dispatch error response", e)
        }
    }

    /**
     * Called when the USER cancels-rejects the transaction from the UI.
     * Uses the cached resolved request object to send the rejection.
     */
    fun reject() {
        scope.launch {
            try {
                val request = activeRequestObject

                if (request == null) {
                    logger?.d(TAG, "Attempted to reject, but no active request found.")
                    return@launch
                }

                val wrapper = OpenId4VpRequest(request)
                val encParams = wrapper.responseEncryptionParameters

                logger?.d(TAG, "User rejected the request. Dispatching error.")

                // Dispatch openid4vp NegativeConsensus case
                val outcome = openId4Vp.dispatch(
                    request = request,
                    consensus = Consensus.NegativeConsensus,
                    encryptionParameters = encParams
                )

                if (outcome is DispatchOutcome.VerifierResponse.Accepted) {
                    logger?.d(TAG, "Rejection accepted by verifier.")

                    val redirectUri = outcome.redirectURI
                    if (redirectUri != null) {
                        // Verifier wants us to redirect
                        transferEventListeners.onTransferEvent(TransferEvent.Redirect(redirectUri))
                    } else {
                        // Verifier just said OK
                        transferEventListeners.onTransferEvent(TransferEvent.ResponseSent)
                    }
                } else {
                    logger?.d(TAG, "Outcome is $outcome")
                }

                activeRequestObject = null
            }  catch (e: Throwable) {
                logger?.e(TAG, "Failed to send rejection response", e)
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

                logger?.let { response.debugLog(it, TAG) }

                when (val outcome = openId4Vp.dispatch(
                    request = response.resolvedRequestObject,
                    consensus = response.vpToken,
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
        activeRequestObject = null
        scope.coroutineContext.cancelChildren()
    }

    private fun List<TransferEvent.Listener>.onTransferEvent(
        event: TransferEvent,
    ) {
        val executor = listenersExecutor ?: Dispatchers.Main.asExecutor()
        transferEventListeners.forEach { executor.execute { it.onTransferEvent(event) } }
    }

    companion object {
        private const val TAG = "OpenId4VpManager"

        private val DefaultHttpClientFactory: () -> HttpClient = {
            HttpClient {
                install(ContentNegotiation) { json() }
                expectSuccess = true
            }
        }

        /**
         * Prints detailed debug information about the response.
         *
         * @param logger The Logger instance to use for logging
         * @param tag The tag to use for logging the response information
         */
        private fun OpenId4VpResponse.debugLog(logger: Logger, tag: String) {
            val message = "Response: ${
                respondedDocuments.map { (queryId, respStr) -> "$queryId: $respStr" }
                    .joinToString("\n")
            }"
            logger.d(tag, message)
            logger.d(tag, "VpContent: ${vpToken.verifiablePresentations}")
        }
    }
}