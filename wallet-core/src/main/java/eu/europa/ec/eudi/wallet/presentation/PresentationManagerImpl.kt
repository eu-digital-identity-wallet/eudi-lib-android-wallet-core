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
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.wallet.presentation.SessionTerminationFlag.Companion.SEND_SESSION_TERMINATION_MESSAGE
import eu.europa.ec.eudi.wallet.presentation.SessionTerminationFlag.Companion.USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import org.jetbrains.annotations.VisibleForTesting

/**
 * Implementation of the [PresentationManager] interface based on the [TransferManager]
 * and [OpenId4VpManager] implementations.
 * @property nfcEngagementServiceClass the NFC engagement service class
 * @property readerTrustStore the reader trust store
 */
class PresentationManagerImpl @JvmOverloads constructor(
    @VisibleForTesting internal val transferManager: TransferManager,
    @VisibleForTesting internal val openId4vpManager: OpenId4VpManager? = null,
    override val nfcEngagementServiceClass: Class<out NfcEngagementService>? = null,
) : PresentationManager {

    private var _readerTrustStore: ReaderTrustStore? = null
    override var readerTrustStore: ReaderTrustStore?
        get() = _readerTrustStore
        set(value) {
            _readerTrustStore = value
            (transferManager as? ReaderTrustStoreAware)?.readerTrustStore = value
            openId4vpManager?.readerTrustStore = value
        }

    override fun addTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferManager.addTransferEventListener(listener)
        openId4vpManager?.addTransferEventListener(listener)
    }

    override fun removeAllTransferEventListeners() = apply {
        transferManager.removeAllTransferEventListeners()
        openId4vpManager?.removeAllTransferEventListeners()
    }

    override fun removeTransferEventListener(listener: TransferEvent.Listener) = apply {
        transferManager.removeTransferEventListener(listener)
        openId4vpManager?.removeTransferEventListener(listener)
    }

    override fun startProximityPresentation() {
        transferManager.startQrEngagement()
    }

    override fun startRemotePresentation(intent: Intent) {
        val uri = intent.data ?: throw IllegalArgumentException("Intent data is missing")
        when {
            uri.scheme == MDOC_SCHEME ->
                transferManager.startEngagementToApp(intent)


            true == openId4vpManager?.config?.schemes?.contains(uri.scheme) ->
                openId4vpManager.resolveRequestUri(uri.toString())

            else -> throw IllegalStateException("Not supported scheme")
        }
    }

    override fun startRemotePresentation(uri: Uri, refererUrl: String?) {
        startRemotePresentation(Intent().apply {
            data = uri
            refererUrl?.let { putExtra(Intent.EXTRA_REFERRER, it) }
        })
    }

    override fun enableNFCEngagement(
        activity: ComponentActivity,
    ) = apply {
        NfcEngagementService.enable(activity, nfcEngagementServiceClass)
    }

    override fun disableNFCEngagement(activity: ComponentActivity) = apply {
        NfcEngagementService.disable(activity)
    }

    override fun sendResponse(response: Response) {
        when (response) {
            is DeviceResponse -> transferManager.sendResponse(response)

            is OpenId4VpResponse.DeviceResponse -> openId4vpManager?.sendResponse(response)

            is OpenId4VpResponse.GenericResponse -> openId4vpManager?.sendResponse(response)

            else -> throw IllegalStateException("Unable to determine the presentation mode")
        }
    }

    override fun stopProximityPresentation(@SessionTerminationFlag flags: Int) {
        transferManager.stopPresentation(
            sendSessionTerminationMessage = flags and SEND_SESSION_TERMINATION_MESSAGE != 0,
            useTransportSpecificSessionTermination = flags and USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION != 0
        )
    }

    override fun stopRemotePresentation() {
        openId4vpManager?.stop()
    }


    companion object {
        const val MDOC_SCHEME = "mdoc"
    }


}