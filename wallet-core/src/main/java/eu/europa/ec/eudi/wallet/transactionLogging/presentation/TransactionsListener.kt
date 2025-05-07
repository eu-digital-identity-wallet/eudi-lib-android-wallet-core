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
 * Listener for transaction logging
 *
 * Observes transfer events and logs transaction details using TransactionLogBuilder
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

    private val logBuilder = TransactionLogBuilder(metadataResolver)

    /**
     * The transaction log for the current transaction
     */
    internal var log: TransactionLog = logBuilder.createEmptyPresentationLog()

    /**
     * Logs the response after sending it to the relying party
     *
     * @param response the response to be logged
     * @param error the exception that occurred during sending the response, if any
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
     * Handle transfer events
     *
     * Updates the log based on the event type:
     * - [TransferEvent.Connected] - resets the log
     * - [TransferEvent.RequestReceived] - updates with request and relying party info
     * - [TransferEvent.Error] - marks log with error status
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

            else -> Unit
        }
    }

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
