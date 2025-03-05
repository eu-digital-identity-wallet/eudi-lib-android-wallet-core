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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Listener for transaction logging
 */
class TransactionsListener(
    private val transactionLogger: TransactionLogger,
    documentManager: DocumentManager,
    private val logger: Logger? = null,
) : TransferEvent.Listener {

    /**
     * Mutex to ensure thread safety when updating the log
     */
    private val mutex = Mutex()

    /**
     * The transaction log for the current transaction
     */
    internal var log: TransactionLog = TransactionLog(
        timestamp = Instant.now().toEpochMilli(),
        status = TransactionLog.Status.Incomplete,
        type = TransactionLog.Type.Presentation,
        relyingParty = null,
        rawRequest = null,
        rawResponse = null,
        dataFormat = null,
        sessionTranscript = null,
        metadata = null,
    )

    /**
     * Resolver for metadata
     */
    internal val metadataResolver: (List<DocumentId>) -> List<String?> = { ids ->
        ids.map {
            documentManager.getDocumentById(it)?.metadata?.toJson()
        }
    }

    /**
     * Logs the response after sending it to the relying party
     * This method is called from [TransactionsDecorator.sendResponse]
     *
     * @param response the response to be logged
     * @param e the exception that occurred during sending the response, if any
     */
    fun logResponse(response: Response, e: Throwable? = null) {
        log = log.updateLog {
            withResponse(response, metadataResolver, e)
        }
        tryLog(log)
    }

    /**
     * Listen for transfer events
     *
     * Updates the log based on the event type.
     * - [TransferEvent.Connected] - resets the log
     * - [TransferEvent.RequestReceived] - updates the log with the request and relying party
     * - [TransferEvent.Error] - updates the log with the error status
     */
    override fun onTransferEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.Connected -> log = log.updateLog {
                copy(
                    status = TransactionLog.Status.Incomplete,
                    type = TransactionLog.Type.Presentation,
                    timestamp = Instant.now().toEpochMilli()
                )
            }

            is TransferEvent.RequestReceived -> {
                log = log.updateLog {
                    withRequest(event.request).withRelyingParty(event.processedRequest)
                }

            }

            is TransferEvent.Error -> {
                log = log.updateLog {
                    copy(
                        timestamp = Instant.now().toEpochMilli(),
                        status = TransactionLog.Status.Error
                    )
                }
                tryLog(log)
            }

            else -> Unit
        }
    }

    /**
     * Tries to log the transaction
     *
     * If logging fails, it logs an error message using the provided logger
     *
     * @param log the transaction log to be logged
     */
    private fun tryLog(log: TransactionLog) {
        try {
            transactionLogger.log(log)
        } catch (e: Throwable) {
            logger?.log(
                Logger.Record(
                    level = Logger.LEVEL_ERROR,
                    message = "Failed to log transaction",
                    thrown = e,
                    sourceClassName = TransactionsListener::class.java.name,
                    sourceMethod = "tryLog"
                )
            )
        }
    }

    /**
     * Updates the transaction log with the provided update function
     *
     * @param update the update function to be applied to the log
     * @return the updated transaction log
     */
    private fun TransactionLog.updateLog(update: TransactionLog.() -> TransactionLog): TransactionLog {
        return runBlocking {
            mutex.withLock {
                try {
                    update(this@updateLog)
                } catch (e: Throwable) {
                    logger?.log(
                        Logger.Record(
                            level = Logger.LEVEL_ERROR,
                            message = "Failed to update transaction log",
                            thrown = e,
                            sourceClassName = TransactionsListener::class.java.name,
                            sourceMethod = "updateLog"
                        )
                    )
                    this@updateLog.copy(status = TransactionLog.Status.Error)
                }
            }
        }
    }
}