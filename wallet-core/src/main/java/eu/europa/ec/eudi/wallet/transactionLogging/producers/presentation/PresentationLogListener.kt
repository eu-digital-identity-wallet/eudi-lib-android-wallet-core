/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.producers.toNoncompletionReason
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult

/**
 * Listens to transfer events and records [TransactionEntry.Presentation] entries through the
 * [TransactionLogManager]. It stores only claim identifiers and paths, never raw bytes or values.
 *
 * @property transactionLogManager records entries.
 * @property logger optional logger for internal errors.
 */
class PresentationLogListener(
    private val transactionLogManager: TransactionLogManager,
    private val logger: Logger? = null,
) : TransferEvent.Listener {

    /**
     * Builder for creating and updating presentation entries.
     */
    internal var logBuilder = PresentationLogBuilder()

    /**
     * The entry for the current transaction.
     */
    internal var log: TransactionEntry.Presentation = logBuilder.createEmptyPresentationLog()

    /**
     * Whether the current transaction has already been recorded, to avoid double-logging.
     */
    internal var finalized: Boolean = false

    /**
     * Whether the current session got a request the wallet could process. A presentation is logged
     * only once such a request arrives; until then, stop/disconnect/error events are ignored, so a
     * wrong-type scan, a connection blip, or an unparseable request does not create an empty entry.
     */
    internal var hasProcessableRequest: Boolean = false

    /**
     * Records the response after it is sent to the relying party.
     *
     * @param response the response that was sent.
     * @param error the error that occurred while sending, if any.
     */
    fun logResponse(response: Response, error: Throwable? = null) {
        try {
            log = logBuilder.withResponse(log, response, error)
            transactionLogManager.log(log)
            finalized = true
        } catch (e: Throwable) {
            logError(e, "logResponse")
            log = logBuilder.withError(log, e.toNoncompletionReason(REASON_LOGGING_ERROR))
            transactionLogManager.log(log)
            finalized = true
        }
    }

    /**
     * Records the presented claims of a dispatched response without deciding the result. A later
     * transfer event marks the entry completed or not completed, so the entry is not finalized here.
     *
     * @param response the response that was dispatched.
     */
    fun recordResponse(response: Response) {
        try {
            log = logBuilder.withPresentedClaims(log, response)
            transactionLogManager.log(log)
        } catch (e: Throwable) {
            logError(e, "recordResponse")
        }
    }

    /**
     * Records the current transaction as not completed if it has not already been recorded.
     *
     * Typically called when a presentation is stopped or disconnected before completion.
     */
    fun logStopped() {
        try {
            // Only log a stop if a real request was received; otherwise there is no transaction.
            if (!finalized && hasProcessableRequest) {
                // Preserve a more specific reason captured earlier (e.g. an unsupported request),
                // otherwise record that the presentation was stopped before completion.
                val existingReason =
                    (log.transactionResult as? TransactionResult.NotCompleted)?.reason
                log = logBuilder.withError(log, existingReason ?: REASON_STOPPED)
                transactionLogManager.log(log)
                finalized = true
            }
        } catch (e: Throwable) {
            logError(e, "logStopped")
        }
    }

    /**
     * Handles transfer events to build/record the transaction entry.
     */
    override fun onTransferEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.Connected -> {
                log = logBuilder.createEmptyPresentationLog()
                finalized = false
                hasProcessableRequest = false
            }

            is TransferEvent.RequestReceived -> {
                try {
                    log = logBuilder.createEmptyPresentationLog()
                    finalized = false
                    // A transaction exists only if the request could be processed; otherwise it is a
                    // wrong-type or garbage scan that should not be logged.
                    hasProcessableRequest = event.processedRequest.getOrNull() != null
                    log = logBuilder.withRequest(log, event.request, event.processedRequest)
                    log = logBuilder.withRelyingParty(log, event.processedRequest)
                } catch (e: Throwable) {
                    logError(e, "onTransferEvent: RequestReceived")
                    log = logBuilder.withError(log, e.toNoncompletionReason(REASON_REQUEST_ERROR))
                }
                // Log the request right away as not completed, so a presentation that is abandoned
                // with no later event is still recorded. The entry id stays the same, so the events
                // below update this same row.
                if (hasProcessableRequest) writePendingEntry()
            }

            is TransferEvent.Error -> {
                try {
                    if (!finalized && hasProcessableRequest) {
                        log = logBuilder.withError(log, event.error.toNoncompletionReason(REASON_TRANSFER_ERROR))
                        transactionLogManager.log(log)
                        finalized = true
                    }
                } catch (e: Throwable) {
                    logError(e, "onTransferEvent: Error")
                }
            }

            // The response left the wallet, so the presentation completed. The presented claims were
            // already recorded at send time. The success event differs per mode: OpenID4VP/proximity
            // use ResponseSent or a success Redirect, DCAPI uses IntentToSend.
            TransferEvent.ResponseSent,
            is TransferEvent.Redirect,
            is TransferEvent.IntentToSend -> {
                try {
                    if (!finalized && hasProcessableRequest) {
                        log = logBuilder.withCompleted(log)
                        transactionLogManager.log(log)
                        finalized = true
                    }
                } catch (e: Throwable) {
                    logError(e, "onTransferEvent: response sent")
                }
            }

            TransferEvent.Disconnected -> logStopped()

            else -> Unit
        }
    }

    /**
     * Logs the current not-completed entry without finalizing it, so it survives an abandoned flow.
     * Failures are ignored; a later event records the outcome if the flow continues.
     */
    private fun writePendingEntry() {
        try {
            transactionLogManager.log(log)
        } catch (e: Throwable) {
            logError(e, "writePendingEntry")
        }
    }

    private fun logError(e: Throwable, source: String) {
        logger?.log(
            Logger.Record(
                level = Logger.LEVEL_ERROR,
                message = "Failed to log transaction",
                thrown = e,
                sourceClassName = PresentationLogListener::class.java.name,
                sourceMethod = source
            )
        )
    }

    companion object {
        /** Reason recorded when a presentation is stopped/disconnected before a response is sent. */
        internal const val REASON_STOPPED = "Presentation stopped before completion"

        /** Fallback reason when a transfer error carries no message. */
        internal const val REASON_TRANSFER_ERROR = "Transfer error"

        /** Fallback reason when processing the received request throws without a message. */
        internal const val REASON_REQUEST_ERROR = "Failed to process the request"

        /** Fallback reason when recording the response throws without a message. */
        internal const val REASON_LOGGING_ERROR = "Failed to record the response"
    }
}
