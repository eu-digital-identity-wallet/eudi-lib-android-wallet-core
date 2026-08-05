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
    internal lateinit var onConnecting: () -> Unit
    internal lateinit var onDeviceRetrievalHelperReady: (deviceRetrievalHelper: DeviceRetrievalHelper) -> Unit
    internal lateinit var onNewDeviceRequest: (request: ByteArray) -> Unit
    internal lateinit var onDisconnected: (transportSpecificTermination: Boolean) -> Unit
    internal lateinit var onCommunicationError: (error: Throwable) -> Unit

    private lateinit var nfcEngagement: NfcEngagementHelper

    private var deviceRetrievalHelper: DeviceRetrievalHelper? = null

    private lateinit var eDevicePrivateKey: EcPrivateKey

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

    private val nfcEngagementListener = object : NfcEngagementHelper.Listener {
        override fun onTwoWayEngagementDetected() {
            Log.d(this.TAG, "Engagement Listener: Two Way Engagement Detected.")
        }

        override fun onDeviceConnecting() {
            Log.d(this.TAG, "NFC Engagement: Device Connecting")
            onConnecting()
        }

        override fun onDeviceConnected(transport: DataTransport) {
            if (deviceRetrievalHelper != null) {
                Log.d(
                    TAG,
                    "Engagement Listener: Device Connected -> ignored due to active presentation",
                )
                return
            }

            Log.d(this.TAG, "onDeviceConnected via NFC: nfcEngagement=$nfcEngagement")

            val builder = DeviceRetrievalHelper.Builder(
                applicationContext,
                deviceRetrievalHelperListener,
                application.mainExecutor(),
                eDevicePrivateKey,
            )
            builder.useForwardEngagement(
                transport,
                nfcEngagement.deviceEngagement,
                nfcEngagement.handover,
            )
            deviceRetrievalHelper = builder.build()
            nfcEngagement.close()
            onDeviceRetrievalHelperReady(requireNotNull(deviceRetrievalHelper))
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

    private val deviceRetrievalHelperListener = object : DeviceRetrievalHelper.Listener {
        override fun onEReaderKeyReceived(eReaderKey: EcPublicKey) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onEReaderKeyReceived")
        }

        override fun onDeviceRequest(deviceRequestBytes: ByteArray) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onDeviceRequest")
            onNewDeviceRequest(deviceRequestBytes)
        }

        override fun onDeviceDisconnected(transportSpecificTermination: Boolean) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onDeviceDisconnected")
            onDisconnected(transportSpecificTermination)
        }

        override fun onError(error: Throwable) {
            Log.d(this.TAG, "DeviceRetrievalHelper Listener (NFC): onError -> ${error.message}")
            onCommunicationError(error)
        }
    }

    override fun onCreate() {
        super.onCreate()
        eDevicePrivateKey = runBlocking {
            Crypto.createEcPrivateKey(EcCurve.P256)
        }
        transferManager.setupNfcEngagement(this)
        nfcEngagement = NfcEngagementHelper.Builder(
            applicationContext,
            eDevicePrivateKey.publicKey,
            retrievalMethods.transportOptions,
            nfcEngagementListener,
            applicationContext.mainExecutor(),
        ).apply {
            useStaticHandover(retrievalMethods.connectionMethods)
        }.build()
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        return nfcEngagement.nfcProcessCommandApdu(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
        nfcEngagement.nfcOnDeactivated(reason)
        val timeoutSeconds = 15
        Handler(Looper.getMainLooper()).postDelayed({
            nfcEngagement.close()
        }, timeoutSeconds * 1000L)
    }
}
