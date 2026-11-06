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
package eu.europa.ec.eudi.iso18013.transfer.engagement

import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.engagement.NfcEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransport
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.internal.TAG
import eu.europa.ec.eudi.iso18013.transfer.internal.connectionMethods
import eu.europa.ec.eudi.iso18013.transfer.internal.mainExecutor
import eu.europa.ec.eudi.iso18013.transfer.internal.transportOptions
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.nfc.Nfc
import org.multipaz.nfc.ResponseApdu

/**
 * Abstract Nfc engagement service.
 *
 * Implement this class to enable the NFC engagement.
 *
 * ```
 * class MyNfcEngagementService : NfcEngagementService() {
 *   override val transferManager: TransferManager = TransferManagerImpl()
 * }
 *```
 *
 * then add to application manifest file:
 *
 * ```
 * <service android:name=".MyNfcEngagementService"
 *         android:exported="true"
 *             android:label="@string/nfc_engagement_service_desc"
 *             android:permission="android.permission.BIND_NFC_SERVICE">
 *             <intent-filter>
 *                 <action android:name="android.nfc.action.NDEF_DISCOVERED" />
 *                 <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE"/>
 *             </intent-filter>
 *
 *             <meta-data
 *                 android:name="android.nfc.cardemulation.host_apdu_service"
 *                 android:resource="@xml/nfc_engagement_apdu_service" />
 *  </service>
 *  ```
 *
 * You can enable or disable the NFC device engagement in your app by calling the `enable()`
 * and `disable()` methods of the `NfcEngagementService` class.
 *
 * In the example below, the NFC device engagement is enabled when activity is resumed and disabled
 * when the activity is paused.
 *
 * ```
 * import androidx.appcompat.app.AppCompatActivity
 * import eu.europa.ec.eudi.iso18013.holder.transfer.engagement.NfcEngagementService
 *
 * class MainActivity : AppCompatActivity() {
 *
 *   override fun onResume() {
 *     super.onResume()
 *     NfcEngagementService.enable(this)
 *   }
 *
 *   override fun onPause() {
 *     super.onPause()
 *     NfcEngagementService.disable(this)
 *   }
 * }
 * ```
 * Optionally, in the `enable()` method you can define your class that implements `NfcEngagementService`, e.g.:
 *
 *```
 * NfcEngagementService.enable(this, NfcEngagementServiceImpl::class.java)
 *```
 *
 * This defines the nfc engagement service to be preferred while this activity is in the foreground.
 *
 * @constructor
 */
abstract class NfcEngagementService : HostApduService() {

    /**
     * Transfer manager
     *
     * @return a [TransferManager]
     */
    abstract val transferManager: TransferManager

    internal lateinit var retrievalMethods: List<DeviceRetrievalMethod>
    internal lateinit var isPresentationActive: () -> Boolean
    internal lateinit var onConnecting: () -> Unit
    internal lateinit var onDeviceRetrievalHelperReady: (deviceRetrievalHelper: DeviceRetrievalHelper) -> Unit
    internal lateinit var onNewDeviceRequest: (deviceRetrievalHelper: DeviceRetrievalHelper, request: ByteArray) -> Unit
    internal lateinit var onDisconnected: (transportSpecificTermination: Boolean) -> Unit
    internal lateinit var onSessionEnded: (deviceRetrievalHelper: DeviceRetrievalHelper) -> Unit
    internal lateinit var onCommunicationError: (error: Throwable) -> Unit

    /**
     * The engagement of the current NFC tap, or of the last tap while an mdoc verifier may still
     * connect to it; null when there is none.
     */
    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var engagement: Engagement? = null

    /** Whether an NFC tap is in progress, from its first command until [onDeactivated]. */
    private var isTapActive = false

    /** Builds the NFC engagement helper of an [Engagement]. */
    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var buildNfcEngagementHelper: (
        eDevicePrivateKey: EcPrivateKey,
        listener: NfcEngagementHelper.Listener,
    ) -> NfcEngagementHelper = { eDevicePrivateKey, listener ->
        NfcEngagementHelper.Builder(
            applicationContext,
            eDevicePrivateKey.publicKey,
            retrievalMethods.transportOptions,
            listener,
            applicationContext.mainExecutor(),
        ).apply {
            useStaticHandover(retrievalMethods.connectionMethods)
        }.build()
    }

    /** Builds the device retrieval helper of a connection made through an [Engagement]. */
    @JvmSynthetic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var buildDeviceRetrievalHelper: (
        engagement: Engagement,
        transport: DataTransport,
        listener: DeviceRetrievalHelper.Listener,
    ) -> DeviceRetrievalHelper = { engagement, transport, listener ->
        DeviceRetrievalHelper.Builder(
            applicationContext,
            listener,
            application.mainExecutor(),
            engagement.eDevicePrivateKey,
        ).useForwardEngagement(
            transport,
            engagement.helper.deviceEngagement,
            engagement.helper.handover,
        ).build()
    }

    companion object {

        /**
         * Enable NFC engagement
         *
         * @param activity
         */
        @JvmStatic
        fun enable(
            activity: ComponentActivity,
            preferredNfcEngSerCls: Class<out NfcEngagementService>? = null,
        ) {
            // set preferred Nfc Engagement Service
            preferredNfcEngSerCls?.let {
                setAsPreferredNfcEngagementService(activity, preferredNfcEngSerCls)
            }
        }

        /**
         * Disable NFC engagement
         *
         * @param activity
         */
        @JvmStatic
        fun disable(activity: ComponentActivity) {
            // unset preferred Nfc Engagement Service
            unsetAsPreferredNfcEngagementService(activity)
        }

        @JvmStatic
        private fun setAsPreferredNfcEngagementService(
            activity: ComponentActivity,
            nfcEngagementServiceClass: Class<out NfcEngagementService>,
        ) {
            val cardEmulation = CardEmulation.getInstance(NfcAdapter.getDefaultAdapter(activity))
            val allowsForeground =
                cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)
            if (allowsForeground) {
                val hceComponentName = ComponentName(
                    activity,
                    nfcEngagementServiceClass,
                )
                cardEmulation.setPreferredService(activity, hceComponentName)
            }
        }

        @JvmStatic
        private fun unsetAsPreferredNfcEngagementService(activity: ComponentActivity) {
            val cardEmulation = CardEmulation.getInstance(NfcAdapter.getDefaultAdapter(activity))
            val allowsForeground =
                cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)
            if (allowsForeground) {
                cardEmulation.unsetPreferredService(activity)
            }
        }
    }

    /**
     * The device engagement of one NFC tap, with its own ephemeral device key.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal inner class Engagement {

        val eDevicePrivateKey: EcPrivateKey = runBlocking {
            Crypto.createEcPrivateKey(EcCurve.P256)
        }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal val listener = object : NfcEngagementHelper.Listener {
            override fun onTwoWayEngagementDetected() {
                Log.d(this.TAG, "Engagement Listener: Two Way Engagement Detected.")
            }

            override fun onDeviceConnecting() {
                Log.d(this.TAG, "NFC Engagement: Device Connecting")
                onConnecting()
            }

            override fun onDeviceConnected(transport: DataTransport) {
                connect(this@Engagement, transport)
            }

            override fun onError(error: Throwable) {
                Log.d(this.TAG, "NFC onError: ${error.message}")
                onCommunicationError(error)
            }

            override fun onHandoverSelectMessageSent() {
                Log.d(this.TAG, "NFC Engagement: Handover Select Message Sent")
                Log.d(this.TAG, "NFC Engagement: NegotiatedHandover not supported yet")
            }
        }

        val helper: NfcEngagementHelper = buildNfcEngagementHelper(eDevicePrivateKey, listener)

        /**
         * Closes the engagement. When it is the current one, the next tap creates a new one.
         */
        fun close() {
            helper.close()
            if (this@NfcEngagementService.engagement === this) {
                this@NfcEngagementService.engagement = null
            }
        }
    }

    /**
     * Starts the presentation session of an mdoc verifier that connected through [engagement].
     * The connection is ignored while a presentation is active, or when [engagement] is not the
     * current one.
     */
    private fun connect(engagement: Engagement, transport: DataTransport) {
        if (isPresentationActive() || engagement !== this.engagement) {
            Log.d(
                TAG,
                "Engagement Listener: Device Connected -> ignored due to active presentation or ended engagement",
            )
            // The transport of the ignored mdoc verifier is closed, as no one else closes it
            transport.close()
            return
        }

        Log.d(this.TAG, "onDeviceConnected via NFC")

        // The listener is bound to its own device retrieval helper, so that callbacks of an ended
        // session cannot act on a newer one
        lateinit var deviceRetrievalHelper: DeviceRetrievalHelper
        val listener = object : DeviceRetrievalHelper.Listener {
            override fun onEReaderKeyReceived(eReaderKey: EcPublicKey) {
                Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onEReaderKeyReceived")
            }

            override fun onDeviceRequest(deviceRequestBytes: ByteArray) {
                Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onDeviceRequest")
                onNewDeviceRequest(deviceRetrievalHelper, deviceRequestBytes)
            }

            override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
                Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onDeviceDisconnected")
                onSessionEnded(deviceRetrievalHelper)
                onDisconnected(transportSpecificTermination)
            }

            override fun onError(error: Throwable) {
                Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onError -> ${error.message}")
                onSessionEnded(deviceRetrievalHelper)
                onCommunicationError(error)
            }
        }
        deviceRetrievalHelper = buildDeviceRetrievalHelper(engagement, transport, listener)
        // The engagement is used; the next tap creates a new one with a new device key
        engagement.close()
        onDeviceRetrievalHelperReady(deviceRetrievalHelper)
    }

    override fun onCreate() {
        super.onCreate()
        transferManager.setupNfcEngagement(this)
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // While a presentation is active, the engagement is refused before it sets up a
        // data transport for another reader
        if (isPresentationActive()) {
            Log.d(TAG, "NFC engagement refused due to active presentation")
            return ResponseApdu(Nfc.RESPONSE_STATUS_ERROR_FILE_OR_APPLICATION_NOT_FOUND).encode()
        }
        // The first command of a tap starts a new engagement with a new ephemeral device key. The
        // engagement of an earlier tap that no mdoc verifier connected to is closed.
        if (!isTapActive) {
            isTapActive = true
            engagement?.close()
            engagement = Engagement()
        }
        val current = engagement
        if (current == null) {
            Log.d(TAG, "NFC engagement refused: the engagement of this tap has ended")
            return ResponseApdu(Nfc.RESPONSE_STATUS_ERROR_FILE_OR_APPLICATION_NOT_FOUND).encode()
        }
        return current.helper.nfcProcessCommandApdu(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
        val wasTapActive = isTapActive
        isTapActive = false
        if (!wasTapActive) return
        val current = engagement ?: return
        current.helper.nfcOnDeactivated(reason)
        // An mdoc verifier can still connect after the tap; the engagement is closed when none has
        // connected within the timeout. A newer engagement is not affected.
        val timeoutSeconds = 15
        Handler(Looper.getMainLooper()).postDelayed({
            current.close()
        }, timeoutSeconds * 1000L)
    }
}
