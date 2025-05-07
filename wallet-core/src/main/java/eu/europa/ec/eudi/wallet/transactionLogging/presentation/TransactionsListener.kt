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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger

/**
 * Listener for transaction logging.
 *
 * Observes transfer events and logs transaction details using [TransactionLogBuilder].
 * It also handles logging when a presentation is stopped or an error occurs.
 *
 * @property transactionLogger The logger used to persist transaction logs.
 * @param documentManager The manager responsible for accessing document details.
 * @property logger Optional logger for internal logging of the listener itself.
 */
class TransactionsListener(
    private val transactionLogger: TransactionLogger,
    documentManager: DocumentManager,
    private val logger: Logger? = null,
) : TransferEvent.Listener {

    /**
     * Resolver for document metadata
     */
    internal val metadataResolver: (List<DocumentId>) -> List<String?> = { ids ->
        ids.map {
            documentManager.getDocumentById(it)?.metadata?.toJson()
        }
    }

    /**
     * Log builder for creating and updating transaction logs
     */
    internal var logBuilder = TransactionLogBuilder(metadataResolver)

    /**
     * The transaction log for the current transaction
     */
    internal var log: TransactionLog = logBuilder.createEmptyPresentationLog()

    /**
     * Logs the response after sending it to the relying party.
     *
     * This method updates the current transaction log with the response details and
     * any error that might have occurred during the sending process.
     * If an error occurs during logging itself, it logs that error and marks the
     * transaction log with an error status.
     *
     * @param response The response to be logged.
     * @param error The exception that occurred during sending the response, if any.
     */
    fun logResponse(response: Response, error: Throwable? = null) {
        try {
            log = logBuilder.withResponse(log, response, error)
            transactionLogger.log(log)
        } catch (e: Throwable) {
            logError(e, "logResponse")
            log = logBuilder.withError(log)
            transactionLogger.log(log)
        }
    }

    /**
     * Logs that the current transaction has been stopped.
     *
     * If the current transaction log is incomplete (i.e., its status is
     * [TransactionLog.Status.Incomplete]), this method marks the log with an
     * [TransactionLog.Status.Error] status and persists it. This is typically
     * called when a presentation is explicitly stopped or disconnected before completion.
     */
    fun logStopped() {
        try {
            // If the current log is incomplete and presentation is stopped
            if (log.status == TransactionLog.Status.Incomplete) {
                log = logBuilder.withError(log)
                transactionLogger.log(log)
            }
        } catch (e: Throwable) {
            logError(e, "onTransferEvent: Disconnected")
        }
    }

    /**
     * Handles transfer events to update the transaction log.
     *
     * Updates the log based on the event type:
     * - [TransferEvent.Connected]: Resets the log for a new transaction.
     * - [TransferEvent.RequestReceived]: Updates the log with request and relying party information.
     * - [TransferEvent.Error]: Marks the log with an error status and persists it.
     * - [TransferEvent.Disconnected]: Calls [logStopped] to finalize logging for an incomplete transaction.
     *
     * @param event The transfer event to handle.
     */
    override fun onTransferEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.Connected -> {
                log = logBuilder.createEmptyPresentationLog()
            }

            is TransferEvent.RequestReceived -> {
                try {
                    log = logBuilder.withRequest(log, event.request)
                    log = logBuilder.withRelyingParty(log, event.processedRequest)
                } catch (e: Throwable) {
                    logError(e, "onTransferEvent: RequestReceived")
                    log = logBuilder.withError(log)
                }
            }

            is TransferEvent.Error -> {
                try {
                    log = logBuilder.withError(log)
                    transactionLogger.log(log)
                } catch (e: Throwable) {
                    logError(e, "onTransferEvent: Error")
                }
            }

            TransferEvent.Disconnected -> logStopped()

            else -> Unit
        }
    }

    /**
     * Logs an error that occurred within the [TransactionsListener].
     *
     * @param e The throwable to log.
     * @param source The name of the method where the error occurred.
     */
    private fun logError(e: Throwable, source: String) {
        logger?.log(
            Logger.Record(
                level = Logger.LEVEL_ERROR,
                message = "Failed to log transaction",
                thrown = e,
                sourceClassName = TransactionsListener::class.java.name,
                sourceMethod = source
            )
        )
    }
}
