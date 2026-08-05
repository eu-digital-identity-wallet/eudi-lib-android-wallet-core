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
package eu.europa.ec.eudi.iso18013.transfer.internal

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import eu.europa.ec.eudi.iso18013.transfer.engagement.DeviceRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.engagement.QrCode
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey

/**
 * Qr engagement
 *
 * @property context context instance
 * @property onConnecting callback to notify that a new mdoc verifier is connecting
 * @property onQrEngagementReady callback to notify that a the QR Engagement is ready
 * @property onDeviceRetrievalHelperReady callback to notify that the device retrieval helper is ready
 * @property onNewDeviceRequest callback to notify that a device request has been received
 * @property onDisconnected callback to notify that the mdoc verifier has been disconnected
 * @property onCommunicationError callback to notify that an error has been occurred
 * @constructor Create empty Qr engagement
 */
internal class QrEngagement(
    private val context: Context,
    @JvmSynthetic @get:VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    val retrievalMethods: List<DeviceRetrievalMethod>,
    @JvmSynthetic @get:VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    val onConnecting: () -> Unit,
    @JvmSynthetic @get:VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    val onQrEngagementReady: (qrCode: QrCode) -> Unit,
    @JvmSynthetic @get:VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    val onDeviceRetrievalHelperReady: (deviceRetrievalHelper: DeviceRetrievalHelper) -> Unit,
    @JvmSynthetic @get:VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    val onNewDeviceRequest: (request: ByteArray) -> Unit,
    @JvmSynthetic @get:VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    val onDisconnected: (transportSpecificTermination: Boolean) -> Unit,
    @JvmSynthetic @get:VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    val onCommunicationError: (error: Throwable) -> Unit,
) {

    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var deviceRetrievalHelper: DeviceRetrievalHelper? = null

    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal lateinit var eDevicePrivateKey: EcPrivateKey

    @get:JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val qrEngagementListener = object : QrEngagementHelper.Listener {

        override fun onDeviceConnecting() {
            Log.d(this.TAG, "QR Engagement: Device Connecting")
            onConnecting()
        }

        override fun onDeviceConnected(transport: DataTransport) {
            if (deviceRetrievalHelper != null) {
                Log.d(
                    this.TAG,
                    "OnDeviceConnected for QR engagement -> ignoring due to active presentation"
                )
                return
            }

            Log.d(this.TAG, "OnDeviceConnected via QR: qrEngagement=$helper")

            val builder = DeviceRetrievalHelper.Builder(
                context,
                deviceRetrievalHelperListener,
                context.mainExecutor(),
                eDevicePrivateKey,
            )
            builder.useForwardEngagement(
                transport,
                helper.deviceEngagement,
                helper.handover,
            )
            deviceRetrievalHelper = builder.build()
            helper.close()
            onDeviceRetrievalHelperReady(requireNotNull(deviceRetrievalHelper))
        }

        override fun onError(error: Throwable) {
            Log.d(this.TAG, "QR onError: ${error.message}")
            onCommunicationError(error)
        }
    }

    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val deviceRetrievalHelperListener = object : DeviceRetrievalHelper.Listener {
        override fun onEReaderKeyReceived(eReaderKey: EcPublicKey) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (QR): OnEReaderKeyReceived")
        }

        override fun onDeviceRequest(deviceRequestBytes: ByteArray) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (QR): OnDeviceRequest")
            onNewDeviceRequest(deviceRequestBytes)
        }

        override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (QR): onDeviceDisconnected")
            onDisconnected(transportSpecificTermination)
        }

        override fun onError(error: Throwable) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (QR): onError -> ${error.message}")
            onCommunicationError(error)
        }
    }

    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal lateinit var helper: QrEngagementHelper

    val deviceEngagementUriEncoded: String
        get() = helper.deviceEngagementUriEncoded

    /**
     * Configures the QR engagement
     */
    fun configure() {
        eDevicePrivateKey = runBlocking {
            Crypto.createEcPrivateKey(EcCurve.P256)
        }
        helper = QrEngagementHelper.Builder(
            context,
            eDevicePrivateKey.publicKey,
            retrievalMethods.transportOptions,
            qrEngagementListener,
            context.mainExecutor(),
        ).setConnectionMethods(retrievalMethods.connectionMethods)
            .build()
        onQrEngagementReady(QrCode(helper.deviceEngagementUriEncoded))
    }

    /**
     * Closes the connection with the mdoc verifier
     */
    fun close() {
        try {
            helper.close()
        } catch (exception: RuntimeException) {
            Log.e(this.TAG, "Error closing QR engagement", exception)
        }
    }
}
