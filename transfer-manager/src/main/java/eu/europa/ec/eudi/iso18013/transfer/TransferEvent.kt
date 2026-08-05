/*
 * Copyright (c) 2023-2024 European Commission
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
package eu.europa.ec.eudi.iso18013.transfer

import android.content.Intent
import eu.europa.ec.eudi.iso18013.transfer.engagement.QrCode
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import java.net.URI

/**
 * Transfer event
 */
sealed interface TransferEvent {
    /**
     * Qr engagement ready event. This event is triggered when the QR code is ready to be displayed.
     * @property qrCode the QR code
     */
    data class QrEngagementReady(val qrCode: QrCode) : TransferEvent

    /**
     * Connecting event. This event is triggered when the transfer is connecting.
     */
    data object Connecting : TransferEvent

    /**
     * Connected event. This event is triggered when the transfer is connected.
     */
    data object Connected : TransferEvent

    /**
     * Request received event. This event is triggered when the request is received.
     * @property processedRequest the processed request containing the requested documents
     * @property request the request containing the raw data received
     */
    data class RequestReceived(
        val processedRequest: RequestProcessor.ProcessedRequest,
        val request: Request
    ) : TransferEvent

    /**
     * Response sent event. This event is triggered when the response is sent.
     */
    data object ResponseSent : TransferEvent

    /**
     * Redirect event. This event is triggered when a redirect is needed.
     * This event is to be used for implementation for the OpenId4VP protocol.
     * @property redirectUri the redirect URI
     */
    data class Redirect(val redirectUri: URI) : TransferEvent

    /**
     * Intent to send event. This event is triggered when an intent is to be sent.
     * This event is to be used for implementation of Digital Credentials API.
     * @property intent the intent to be sent
     */
    data class IntentToSend(val intent: Intent) : TransferEvent

    /**
     * Disconnected event. This event is triggered when the transfer is disconnected.
     */
    data object Disconnected : TransferEvent

    /**
     * Error event. This event is triggered when an error occurs.
     * @property error the error
     */
    data class Error(val error: Throwable) : TransferEvent

    /**
     * Interface for transfer event listener
     */
    fun interface Listener {
        /**
         * On transfer event callback
         */
        fun onTransferEvent(event: TransferEvent)
    }

    /**
     * Interface for events listenable
     */
    interface Listenable {
        /**
         * Add transfer event listener
         *
         * @param listener
         * @return a [Listenable]
         */
        fun addTransferEventListener(listener: Listener): Listenable

        /**
         * Remove transfer event listener
         *
         * @param listener
         * @return a [Listenable]
         */
        fun removeTransferEventListener(listener: Listener): Listenable

        /**
         * Remove all transfer event listeners
         *
         * @return a [Listenable]
         */
        fun removeAllTransferEventListeners(): Listenable
    }
}
