//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWalletConfig](../index.md)/[Builder](index.md)

# Builder

class [Builder](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))

Builder

#### Parameters

androidJvm

| |
|---|
| context |

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))<br>Create Builder |

## Functions

| Name | Summary |
|---|---|
| [bleClearCacheEnabled](ble-clear-cache-enabled.md) | [androidJvm]<br>fun [bleClearCacheEnabled](ble-clear-cache-enabled.md)(bleClearCacheEnabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [EudiWalletConfig.Builder](index.md)<br>Ble clear cache enabled. If true, the BLE cache will be cleared after each transfer. |
| [bleTransferMode](ble-transfer-mode.md) | [androidJvm]<br>fun [bleTransferMode](ble-transfer-mode.md)(vararg bleTransferMode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EudiWalletConfig.Builder](index.md)<br>Ble transfer mode. This is the BLE transfer mode. It can be [BLE_SERVER_PERIPHERAL_MODE](../-companion/-b-l-e_-s-e-r-v-e-r_-p-e-r-i-p-h-e-r-a-l_-m-o-d-e.md), [BLE_CLIENT_CENTRAL_MODE](../-companion/-b-l-e_-c-l-i-e-n-t_-c-e-n-t-r-a-l_-m-o-d-e.md) or both. |
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [EudiWalletConfig](../index.md)<br>Build the [EudiWalletConfig](../index.md) object |
| [documentsStorageDir](documents-storage-dir.md) | [androidJvm]<br>fun [documentsStorageDir](documents-storage-dir.md)(documentStorageDir: [File](https://developer.android.com/reference/kotlin/java/io/File.html)): [EudiWalletConfig.Builder](index.md)<br>Documents storage dir. This is the directory where the documents will be stored. If not set, the default directory is the noBackupFilesDir. |
| [encryptDocumentsInStorage](encrypt-documents-in-storage.md) | [androidJvm]<br>fun [encryptDocumentsInStorage](encrypt-documents-in-storage.md)(encryptDocumentsInStorage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [EudiWalletConfig.Builder](index.md)<br>Encrypt documents in storage. If true, the documents will be encrypted in the storage. |
| [openId4VciConfig](open-id4-vci-config.md) | [androidJvm]<br>fun [openId4VciConfig](open-id4-vci-config.md)(openId4VciConfig: [OpenId4VciConfig](../../../eu.europa.ec.eudi.wallet.document.issue.openid4vci/-open-id4-vci-config/index.md)): [EudiWalletConfig.Builder](index.md)<br>fun [openId4VciConfig](open-id4-vci-config.md)(block: [OpenId4VciConfig.Builder](../../../eu.europa.ec.eudi.wallet.document.issue.openid4vci/-open-id4-vci-config/-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [EudiWalletConfig.Builder](index.md)<br>OpenId4Vci config |
| [openId4VpConfig](open-id4-vp-config.md) | [androidJvm]<br>fun [openId4VpConfig](open-id4-vp-config.md)(openId4VpConfig: [OpenId4VpConfig](../../../eu.europa.ec.eudi.wallet.transfer.openid4vp/-open-id4-vp-config/index.md)): [EudiWalletConfig.Builder](index.md)<br>fun [openId4VpConfig](open-id4-vp-config.md)(block: [OpenId4VpConfig.Builder](../../../eu.europa.ec.eudi.wallet.transfer.openid4vp/-open-id4-vp-config/-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [EudiWalletConfig.Builder](index.md)<br>openId4VpConfig config |
| [trustedReaderCertificates](trusted-reader-certificates.md) | [androidJvm]<br>fun [trustedReaderCertificates](trusted-reader-certificates.md)(vararg trustedReaderCertificates: [X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)): [EudiWalletConfig.Builder](index.md)<br>fun [trustedReaderCertificates](trusted-reader-certificates.md)(trustedReaderCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [EudiWalletConfig.Builder](index.md)<br>Trusted reader certificates. This is the list of trusted reader certificates.<br>[androidJvm]<br>fun [trustedReaderCertificates](trusted-reader-certificates.md)(@[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg rawIds: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EudiWalletConfig.Builder](index.md)<br>Trusted reader certificates This is the list of trusted reader certificates as raw resource ids. |
| [useHardwareToStoreKeys](use-hardware-to-store-keys.md) | [androidJvm]<br>fun [useHardwareToStoreKeys](use-hardware-to-store-keys.md)(useHardwareToStoreKeys: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [EudiWalletConfig.Builder](index.md)<br>Use hardware to store keys. If true and supported by device, documents' keys will be stored in the hardware. |
| [userAuthenticationRequired](user-authentication-required.md) | [androidJvm]<br>fun [userAuthenticationRequired](user-authentication-required.md)(userAuthenticationRequired: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [EudiWalletConfig.Builder](index.md)<br>User authentication required. If true, the user will be asked to authenticate before accessing the documents' attestations. |
| [userAuthenticationTimeOut](user-authentication-time-out.md) | [androidJvm]<br>fun [userAuthenticationTimeOut](user-authentication-time-out.md)(userAuthenticationTimeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): [EudiWalletConfig.Builder](index.md)<br>User authentication time out. This is the time out for the user authentication. |
| [verifyMsoPublicKey](verify-mso-public-key.md) | [androidJvm]<br>fun [verifyMsoPublicKey](verify-mso-public-key.md)(verifyMsoPublicKey: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [EudiWalletConfig.Builder](index.md)<br>Verify that the MSO public key is the same as the one used to issue the document. If true, the MSO public key will be verified against the public key that is used to issue the document. The default value is true. |

## Properties

| Name | Summary |
|---|---|
| [bleClearCacheEnabled](ble-clear-cache-enabled.md) | [androidJvm]<br>var [bleClearCacheEnabled](ble-clear-cache-enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If true, the BLE cache will be cleared after each transfer. The default value is false. |
| [bleTransferMode](ble-transfer-mode.md) | [androidJvm]<br>var [bleTransferMode](ble-transfer-mode.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>This is the BLE transfer mode. It can be [BLE_SERVER_PERIPHERAL_MODE](../-companion/-b-l-e_-s-e-r-v-e-r_-p-e-r-i-p-h-e-r-a-l_-m-o-d-e.md), [BLE_CLIENT_CENTRAL_MODE](../-companion/-b-l-e_-c-l-i-e-n-t_-c-e-n-t-r-a-l_-m-o-d-e.md) or both. The default value is [BLE_SERVER_PERIPHERAL_MODE](../-companion/-b-l-e_-s-e-r-v-e-r_-p-e-r-i-p-h-e-r-a-l_-m-o-d-e.md). |
| [documentsStorageDir](documents-storage-dir.md) | [androidJvm]<br>var [documentsStorageDir](documents-storage-dir.md): [File](https://developer.android.com/reference/kotlin/java/io/File.html)<br>This is the directory where the documents will be stored. If not set, the default directory is the noBackupFilesDir. |
| [encryptDocumentsInStorage](encrypt-documents-in-storage.md) | [androidJvm]<br>var [encryptDocumentsInStorage](encrypt-documents-in-storage.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If true, the documents will be encrypted in the storage. The default value is true. |
| [openId4VciConfig](open-id4-vci-config.md) | [androidJvm]<br>var [openId4VciConfig](open-id4-vci-config.md): [OpenId4VciConfig](../../../eu.europa.ec.eudi.wallet.document.issue.openid4vci/-open-id4-vci-config/index.md)?<br>This is the config that will be used to issue using OpenId4Vci. If not set OpenId4Vci will not be available. |
| [openId4VpConfig](open-id4-vp-config.md) | [androidJvm]<br>var [openId4VpConfig](open-id4-vp-config.md): [OpenId4VpConfig](../../../eu.europa.ec.eudi.wallet.transfer.openid4vp/-open-id4-vp-config/index.md)? |
| [trustedReaderCertificates](trusted-reader-certificates.md) | [androidJvm]<br>var [trustedReaderCertificates](trusted-reader-certificates.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;?<br>This is the list of trusted reader certificates. If not set, no reader authentication will be performed. |
| [useHardwareToStoreKeys](use-hardware-to-store-keys.md) | [androidJvm]<br>var [useHardwareToStoreKeys](use-hardware-to-store-keys.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If true and supported by device, documents' keys will be stored in the hardware. The default value is true. |
| [userAuthenticationRequired](user-authentication-required.md) | [androidJvm]<br>var [userAuthenticationRequired](user-authentication-required.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If true, the user will be asked to authenticate before accessing the documents' attestations. The default value is false. |
| [userAuthenticationTimeOut](user-authentication-time-out.md) | [androidJvm]<br>var [userAuthenticationTimeOut](user-authentication-time-out.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>This is the time out for the user authentication. The default value is 30 seconds. |
| [verifyMsoPublicKey](verify-mso-public-key.md) | [androidJvm]<br>var [verifyMsoPublicKey](verify-mso-public-key.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If true, the MSO public key will be verified against the public key that is used to issue the document. The default value is true. |
