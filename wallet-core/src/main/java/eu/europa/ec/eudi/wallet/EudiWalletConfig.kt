/*
 * Copyright (c) 2023 European Commission
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
@file:JvmMultifileClass
package eu.europa.ec.eudi.wallet

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.RawRes
import eu.europa.ec.eudi.wallet.EudiWalletConfig.Builder
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.OpenId4VciConfig
import eu.europa.ec.eudi.wallet.internal.getCertificate
import java.io.File
import java.security.cert.X509Certificate

/**
 * Eudi wallet config.
 *
 * This class is used to configure the Eudi wallet.
 * Use the [Builder] to create an instance of this class.
 *
 * @property builder
 * @constructor Create empty Eudi wallet config
 */

class EudiWalletConfig private constructor(private val builder: Builder) {

    companion object {
        /**
         * Ble Server Peripheral Mode
         */
        const val BLE_SERVER_PERIPHERAL_MODE = 1 shl 0

        /**
         * Ble Client Central Mode
         */
        const val BLE_CLIENT_CENTRAL_MODE = 1 shl 1
    }

    /**
     * Documents storage dir. This is the directory where the documents will be stored.
     * If not set, the default directory is the noBackupFilesDir.
     */
    val documentsStorageDir: File
        get() = builder.documentsStorageDir

    /**
     * Encrypt documents in storage
     * If true, the documents will be encrypted in the storage.
     */
    val encryptDocumentsInStorage: Boolean
        get() = builder.encryptDocumentsInStorage

    /**
     * Use hardware to store keys
     * If true and supported by device, documents' keys will be stored in the hardware.
     *
     * Details: https://source.android.com/docs/security/best-practices/hardware#strongbox-keymaster
     */
    val useHardwareToStoreKeys: Boolean
        get() = builder.useHardwareToStoreKeys

    /**
     * Ble transfer mode
     * This is the BLE transfer mode. It can be [BLE_SERVER_PERIPHERAL_MODE], [BLE_CLIENT_CENTRAL_MODE] or both.
     */
    val bleTransferMode: Int
        get() = builder.bleTransferMode

    /**
     * Ble clear cache enabled
     * If true, the BLE cache will be cleared after each transfer.
     */
    val bleClearCacheEnabled: Boolean
        get() = builder.bleClearCacheEnabled

    /**
     * User authentication required
     * If true, the user will be asked to authenticate before accessing the documents' attestations.
     */
    val userAuthenticationRequired: Boolean
        get() = builder.userAuthenticationRequired

    /**
     * User authentication time out
     * This is the time out for the user authentication.
     */
    val userAuthenticationTimeOut: Long
        get() = builder.userAuthenticationTimeOut

    /**
     * Trusted reader certificates
     * This is the list of trusted reader certificates.
     * If not set, no reader authentication will be performed.
     */
    val trustedReaderCertificates: List<X509Certificate>?
        get() = builder.trustedReaderCertificates

    /**
     * Supports ble central client mode
     * This is true if BLE central client mode is enabled.
     */
    val bleCentralClientModeEnabled: Boolean
        get() = bleTransferMode and BLE_CLIENT_CENTRAL_MODE != 0

    /**
     * Supports ble peripheral server mode
     * This is true if BLE peripheral server mode is enabled.
     */
    val blePeripheralServerModeEnabled: Boolean
        get() = bleTransferMode and BLE_SERVER_PERIPHERAL_MODE != 0

    /**
     * OpenId4Vp verifier uri
     * This is the uri of the verifier that will be used to verify using OpenId4Vp.
     */
    val openId4VpVerifierApiUri: String?
        get() = builder.openId4VpVerifierApiUri

    /**
     * OpenId4Vci config
     * This is the config that will be used to issue using OpenId4Vci.
     */
    val openId4VciConfig: OpenId4VciConfig?
        get() = builder.openId4VciConfig

    /**
     * Builder
     *
     * @constructor
     *
     * @param context
     */
    class Builder(context: Context) {
        private val context: Context = context.applicationContext

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var documentsStorageDir: File = this.context.noBackupFilesDir

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var encryptDocumentsInStorage: Boolean = true

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var useHardwareToStoreKeys: Boolean = true

        @set:JvmSynthetic
        @get:JvmSynthetic
        @BleTransferMode
        internal var bleTransferMode: Int = BLE_SERVER_PERIPHERAL_MODE

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var bleClearCacheEnabled: Boolean = false

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var userAuthenticationRequired: Boolean = false

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var userAuthenticationTimeOut: Long = 30 * 1000

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var trustedReaderCertificates: List<X509Certificate>? = null

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var openId4VpVerifierApiUri: String? = null

        @set:JvmSynthetic
        @get:JvmSynthetic
        internal var openId4VciConfig: OpenId4VciConfig? = null

        /**
         * Documents storage dir. This is the directory where the documents will be stored.
         * If not set, the default directory is the noBackupFilesDir.
         *
         * @param documentStorageDir
         * @return [EudiWalletConfig.Builder]
         */
        fun documentsStorageDir(documentStorageDir: File) = apply {
            this.documentsStorageDir = documentStorageDir
        }

        /**
         * Encrypt documents in storage.
         * If true, the documents will be encrypted in the storage.
         *
         * @param encryptDocumentsInStorage
         * @return [EudiWalletConfig.Builder]
         */
        fun encryptDocumentsInStorage(encryptDocumentsInStorage: Boolean) = apply {
            this.encryptDocumentsInStorage = encryptDocumentsInStorage
        }

        /**
         * Use hardware to store keys.
         * If true and supported by device, documents' keys will be stored in the hardware.
         *
         * @param useHardwareToStoreKeys
         * @return [EudiWalletConfig.Builder]
         */
        fun useHardwareToStoreKeys(useHardwareToStoreKeys: Boolean) = apply {
            this.useHardwareToStoreKeys = useHardwareToStoreKeys
        }

        /**
         * Ble transfer mode.
         * This is the BLE transfer mode. It can be [BLE_SERVER_PERIPHERAL_MODE], [BLE_CLIENT_CENTRAL_MODE] or both.
         *
         * @param bleTransferMode
         * @return [EudiWalletConfig.Builder]
         */
        fun bleTransferMode(@BleTransferMode vararg bleTransferMode: Int) = apply {
            this.bleTransferMode = bleTransferMode.reduce(Int::or)
        }

        /**
         * Ble clear cache enabled.
         * If true, the BLE cache will be cleared after each transfer.
         *
         * @param bleClearCacheEnabled
         * @return [EudiWalletConfig.Builder]
         */
        fun bleClearCacheEnabled(bleClearCacheEnabled: Boolean) = apply {
            this.bleClearCacheEnabled = bleClearCacheEnabled
        }

        /**
         * User authentication required.
         * If true, the user will be asked to authenticate before accessing the documents' attestations.
         *
         * @param userAuthenticationRequired
         * @return [EudiWalletConfig.Builder]
         */
        fun userAuthenticationRequired(userAuthenticationRequired: Boolean) = apply {
            this.userAuthenticationRequired = userAuthenticationRequired
        }

        /**
         * User authentication time out.
         * This is the time out for the user authentication.
         *
         * @param userAuthenticationTimeout
         * @return [EudiWalletConfig.Builder]
         */
        fun userAuthenticationTimeOut(userAuthenticationTimeout: Long) = apply {
            this.userAuthenticationTimeOut = userAuthenticationTimeout
        }

        /**
         * Trusted reader certificates.
         * This is the list of trusted reader certificates.
         *
         * @param trustedReaderCertificates
         * @return [EudiWalletConfig.Builder]
         */
        fun trustedReaderCertificates(trustedReaderCertificates: List<X509Certificate>) = apply {
            this.trustedReaderCertificates = trustedReaderCertificates
        }

        /**
         * Trusted reader certificates
         * This is the list of trusted reader certificates as raw resource ids.
         *
         * @param rawIds raw resource ids of the certificates
         * @return [EudiWalletConfig.Builder]
         */
        fun trustedReaderCertificates(@RawRes vararg rawIds: Int) = apply {
            trustedReaderCertificates(rawIds.map { context.getCertificate(it) })
        }

        /**
         * OpenId4Vp verifier uri
         * This is the uri of the OpenId4Vp verifier
         *
         * @param openId4VpVerifierUri
         * @return [EudiWalletConfig.Builder]
         */
        fun openId4VpVerifierApiUri(openId4VpVerifierUri: String) = apply {
            this.openId4VpVerifierApiUri = openId4VpVerifierUri
        }

        /**
         * OpenId4Vci config
         *
         * @param openId4VciConfig
         */
        fun openId4VciConfig(openId4VciConfig: OpenId4VciConfig) = apply {
            this.openId4VciConfig = openId4VciConfig
        }

        /**
         * OpenId4Vci config
         *
         * @param block
         */
        fun openId4VciConfig(block: OpenId4VciConfig.() -> Unit) = apply {
            this.openId4VciConfig = OpenId4VciConfig().apply(block)
        }

        /**
         * Build the [EudiWalletConfig] object
         *
         * @return []EudiWalletConfig]
         */
        fun build(): EudiWalletConfig = EudiWalletConfig(this)
    }

    /**
     * Ble transfer mode
     *
     * @constructor Create empty Ble transfer mode
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [BLE_SERVER_PERIPHERAL_MODE, BLE_CLIENT_CENTRAL_MODE])
    annotation class BleTransferMode
}
