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

import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.presentation.PresentationManager
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager

/**
 * Wraps a [PresentationManager] and logs presentation transactions. It intercepts sending responses
 * and stopping presentations, recording the details through a [PresentationLogListener].
 *
 * @property delegate the wrapped [PresentationManager].
 * @param transactionLogManager records transaction logs.
 * @param logger optional logger for internal errors.
 */
class PresentationLogger(
    private val delegate: PresentationManager,
    transactionLogManager: TransactionLogManager,
    logger: Logger? = null
) : PresentationManager by delegate {

    /**
     * Listener for logging transactions.
     */
    internal var transactionListener: PresentationLogListener =
        PresentationLogListener(transactionLogManager, logger)

    init {
        // Add the transaction listener to the delegate
        addTransferEventListener(transactionListener)
    }

    /**
     * Sends a response and logs the outcome. On a send failure it logs the failure and re-throws.
     *
     * @param response the response to send.
     * @throws Exception if sending the response fails.
     */
    override fun sendResponse(response: Response) {
        runCatching {
            delegate.sendResponse(response)
        }.onSuccess {
            // The response was dispatched. Record the presented claims now without deciding the
            // result; a later transfer event marks it completed or not. (For OpenID4VP, dispatch is
            // async, so getting here does not yet mean the response was delivered.)
            transactionListener.recordResponse(response)
        }.onFailure {
            // Sending failed right away (e.g. proximity) — log it as not completed and re-throw.
            transactionListener.logResponse(response, it)
            throw it
        }
    }

    /**
     * Stops the proximity presentation and finalizes logging via [PresentationLogListener.logStopped].
     *
     * @param flags passed to the delegate to control stopping.
     */
    override fun stopProximityPresentation(flags: Int) {
        delegate.stopProximityPresentation(flags)
        transactionListener.logStopped()
    }

    /**
     * Stops the remote presentation and finalizes logging via [PresentationLogListener.logStopped].
     */
    override fun stopRemotePresentation() {
        delegate.stopRemotePresentation()
        transactionListener.logStopped()
    }

    /**
     * Removes all transfer event listeners and re-adds the internal [transactionListener], so it
     * stays active even when other listeners are cleared.
     *
     * @return this [PresentationLogger].
     */
    override fun removeAllTransferEventListeners() = apply {
        delegate.removeAllTransferEventListeners()
        addTransferEventListener(transactionListener)
    }
}
