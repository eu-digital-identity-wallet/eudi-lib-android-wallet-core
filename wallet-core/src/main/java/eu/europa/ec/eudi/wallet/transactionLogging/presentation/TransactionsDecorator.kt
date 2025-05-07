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
 * Decorator for [PresentationManager] that logs transactions.
 *
 * This class is responsible for logging the transactions that are sent through the
 * [PresentationManager]. It wraps the original [PresentationManager] and
 * adds logging functionality to it.
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
     * Sends a response and logs it.
     *
     * This method is responsible for sending the response through the
     * [PresentationManager] and logging the response.
     *
     * @param response the response to be sent
     * @throws Exception if an error occurs while sending the response
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

    override fun stopProximityPresentation(flags: Int) {
        delegate.stopProximityPresentation(flags)
        transactionListener.stop()
    }

    override fun stopRemotePresentation() {
        delegate.stopRemotePresentation()
        transactionListener.stop()
    }

    /**
     * Removes all transfer event listeners and adds the transaction listener.
     *
     * @return this instance of [TransactionsDecorator]
     */
    override fun removeAllTransferEventListeners() = apply {
        delegate.removeAllTransferEventListeners()
        addTransferEventListener(transactionListener)
    }
}
