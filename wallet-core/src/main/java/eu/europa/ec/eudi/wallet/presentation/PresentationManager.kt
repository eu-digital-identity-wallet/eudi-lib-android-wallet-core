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

package eu.europa.ec.eudi.wallet.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.presentation.SessionTerminationFlag.Companion.SEND_SESSION_TERMINATION_MESSAGE

/**
 * The PresentationManager is responsible for managing the presentation of the wallet's documents
 * to the verifier.
 * The wallet can present the documents in two ways:
 *
 * - Proximity presentation: the wallet uses BLE/NFC to present the documents to verifier's device
 * - Remote presentation: the wallet sends the documents to the verifier's server
 *
 * This interface extends [TransferEvent.Listenable] that allows to listen to the transfer events
 * through which it receives the requests from the verifier. After receiving the [eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest]
 * the wallet can generate the response with [eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest.Success.generateResponse]
 * and send it back to the verifier by calling [sendResponse] method.
 *
 * It also extends [ReaderTrustStoreAware] that allows to set the [ReaderTrustStore] that is used
 * to verify the authenticity of the reader.
 *
 * It provides also functionality to start the NFC engagement by calling [enableNFCEngagement]
 * method and stop it by calling [disableNFCEngagement] method.
 *
 */
interface PresentationManager : TransferEvent.Listenable, ReaderTrustStoreAware {

    val nfcEngagementServiceClass: Class<out NfcEngagementService>?

    /**
     * Starts the proximity presentation.
     * The QR code is available through the [TransferEvent.QrEngagementReady] event which is triggered
     * almost immediately after calling this method.
     * The wallet should display the QR code to the verifier in order to start the proximity presentation.
     */
    fun startProximityPresentation()

    /**
     * Stops the proximity presentation.
     * Method receives flags that can be used to control the session termination.
     * The available flags are:
     * - [SEND_SESSION_TERMINATION_MESSAGE]: sends the session termination message to the verifier
     * - [SessionTerminationFlag.USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION]: uses the transport specific session termination
     * @param flags the flags
     */
    fun stopProximityPresentation(@SessionTerminationFlag flags: Int = SEND_SESSION_TERMINATION_MESSAGE)

    /**
     * Start a remote presentation with the given URI
     * The URI could be either
     * - a REST API request ISO-18013-7
     * - a OpenId4Vp request
     * @param uri the URI
     */
    fun startRemotePresentation(uri: Uri, refererUrl: String? = null)

    /**
     * Start a remote presentation with the given intent
     * The intent.data could either contain the URI of
     * - a REST API request ISO-18013-7
     * - a OpenId4Vp request
     * @param intent the intent
     */
    fun startRemotePresentation(intent: Intent)

    /**
     * Stops any ongoing remote presentation
     */
    fun stopRemotePresentation()

    /**
     * Starts the DCAPI presentation.
     * This method is used to start the DCAPI presentation flow.
     * @param intent the intent that may contain the DCAPI request.
     */
    fun startDCAPIPresentation(intent: Intent)

    /**
     * Send a response to verifier
     *
     * The response should be generated through the [eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest.Success.generateResponse]
     * method and sent back to the verifier by calling this method.
     *
     * @param response the response
     */
    fun sendResponse(response: Response)

    /**
     * Enable the NFC device engagement for the wallet. This method should be called in the activity's
     * [ComponentActivity.onResume] method.
     *
     * @param activity the activity
     * @return this [EudiWallet] instance
     */
    fun enableNFCEngagement(activity: ComponentActivity): PresentationManager

    /**
     * Disable the NFC device engagement for the wallet. This method should be called in the activity's
     * [ComponentActivity.onPause] method.
     *
     * @param activity the activity
     * @return this [EudiWallet] instance
     */
    fun disableNFCEngagement(activity: ComponentActivity): PresentationManager
}