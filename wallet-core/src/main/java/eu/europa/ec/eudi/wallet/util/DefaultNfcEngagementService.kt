/*
 *  Copyright (c) 2023 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.util

import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.wallet.EudiWallet

/**
 * NFC Engagement Service
 *
 * Add the service to your application's manifest file to enable nfc engagement functionality, as in the following example:
 *
 * ```xml
 *
 * <application>
 *     <!-- rest of manifest -->
 *     <service android:exported="true" android:label="@string/nfc_engagement_service_desc"
 *         android:name="eu.europa.ec.eudi.wallet.util.DefaultNfcEngagementService"
 *         android:permission="android.permission.BIND_NFC_SERVICE">
 *         <intent-filter>
 *             <action android:name="android.nfc.action.NDEF_DISCOVERED" />
 *             <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
 *         </intent-filter>
 *
 *         <!-- the following "@xml/nfc_engagement_apdu_service" in meta-data is provided by the library -->
 *         <meta-data android:name="android.nfc.cardemulation.host_apdu_service"
 *             android:resource="@xml/nfc_engagement_apdu_service" />
 *     </service>
 *
 * </application>
 * ```
 */

class DefaultNfcEngagementService : NfcEngagementService() {
    override val transferManager: TransferManager
        get() = EudiWallet.transferManager
}