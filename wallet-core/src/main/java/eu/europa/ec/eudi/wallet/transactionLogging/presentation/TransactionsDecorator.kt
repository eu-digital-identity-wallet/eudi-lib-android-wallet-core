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

import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.presentation.PresentationManager
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger

/**
 * Decorator for [PresentationManager] that adds transaction logging capabilities.
 *
 * This class wraps an existing [PresentationManager] instance and intercepts
 * key operations like sending responses and stopping presentations to ensure
 * that all relevant transaction details are logged via a [TransactionsListener].
 *
 * @property delegate The underlying [PresentationManager] instance that this decorator wraps.
 * @param documentManager The manager for accessing document details, passed to the [TransactionsListener].
 * @param transactionLogger The logger for persisting transaction logs, passed to the [TransactionsListener].
 * @param logger Optional logger for internal logging of the decorator and listener.
 */
class TransactionsDecorator(
    private val delegate: PresentationManager,
    documentManager: DocumentManager,
    transactionLogger: TransactionLogger,
    logger: Logger? = null
) : PresentationManager by delegate {

    /**
     * Listener for logging transactions.
     */
    internal var transactionListener: TransactionsListener =
        TransactionsListener(transactionLogger, documentManager, logger)

    init {
        // Add the transaction listener to the delegate
        addTransferEventListener(transactionListener)
    }

    /**
     * Sends a response using the delegate [PresentationManager] and logs the outcome.
     *
     * It attempts to send the response and then logs the response details.
     * If sending the response is successful, it logs the response.
     * If an error occurs while sending, it logs the response along with the error
     * and then re-throws the original exception.
     *
     * @param response The response to be sent.
     * @throws Exception if an error occurs while sending the response via the delegate.
     */
    override fun sendResponse(response: Response) {
        runCatching {
            delegate.sendResponse(response)
        }.onSuccess {
            // If the response is sent successfully, log it
            transactionListener.logResponse(response)
        }.onFailure {
            // If an error occurs while sending the response, log it and re-throw
            transactionListener.logResponse(response, it)
            throw it
        }
    }

    /**
     * Stops the proximity presentation using the delegate [PresentationManager]
     * and ensures the transaction logging is appropriately finalized by calling [TransactionsListener.logStopped].
     *
     * @param flags Flags to control the stopping behavior, passed to the delegate.
     */
    override fun stopProximityPresentation(flags: Int) {
        delegate.stopProximityPresentation(flags)
        transactionListener.logStopped()
    }

    /**
     * Stops the remote presentation using the delegate [PresentationManager]
     * and ensures the transaction logging is appropriately finalized by calling [TransactionsListener.logStopped].
     */
    override fun stopRemotePresentation() {
        delegate.stopRemotePresentation()
        transactionListener.logStopped()
    }

    /**
     * Removes all transfer event listeners from the delegate [PresentationManager]
     * and then re-adds the internal [transactionListener].
     *
     * This ensures that the [transactionListener] remains active even if other listeners
     * are cleared.
     *
     * @return This instance of [TransactionsDecorator].
     */
    override fun removeAllTransferEventListeners() = apply {
        delegate.removeAllTransferEventListeners()
        addTransferEventListener(transactionListener)
    }
}
