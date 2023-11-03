//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)

# EudiWalletConfig

[androidJvm]\
class [EudiWalletConfig](index.md)

Eudi wallet config.

This class is used to configure the Eudi wallet. Use the [Builder](-builder/index.md) to create an instance of this class.

## Types

| Name | Summary |
|---|---|
| [BleTransferMode](-ble-transfer-mode/index.md) | [androidJvm]<br>annotation class [BleTransferMode](-ble-transfer-mode/index.md)<br>Ble transfer mode |
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))<br>Builder |
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [bleCentralClientModeEnabled](ble-central-client-mode-enabled.md) | [androidJvm]<br>val [bleCentralClientModeEnabled](ble-central-client-mode-enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Supports ble central client mode This is true if BLE central client mode is enabled. |
| [bleClearCacheEnabled](ble-clear-cache-enabled.md) | [androidJvm]<br>val [bleClearCacheEnabled](ble-clear-cache-enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Ble clear cache enabled If true, the BLE cache will be cleared after each transfer. |
| [blePeripheralServerModeEnabled](ble-peripheral-server-mode-enabled.md) | [androidJvm]<br>val [blePeripheralServerModeEnabled](ble-peripheral-server-mode-enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Supports ble peripheral server mode This is true if BLE peripheral server mode is enabled. |
| [bleTransferMode](ble-transfer-mode.md) | [androidJvm]<br>val [bleTransferMode](ble-transfer-mode.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Ble transfer mode This is the BLE transfer mode. It can be [BLE_SERVER_PERIPHERAL_MODE](-companion/-b-l-e_-s-e-r-v-e-r_-p-e-r-i-p-h-e-r-a-l_-m-o-d-e.md), [BLE_CLIENT_CENTRAL_MODE](-companion/-b-l-e_-c-l-i-e-n-t_-c-e-n-t-r-a-l_-m-o-d-e.md) or both. |
| [documentsStorageDir](documents-storage-dir.md) | [androidJvm]<br>val [documentsStorageDir](documents-storage-dir.md): [File](https://developer.android.com/reference/kotlin/java/io/File.html)<br>Documents storage dir. This is the directory where the documents will be stored. If not set, the default directory is the noBackupFilesDir. |
| [encryptDocumentsInStorage](encrypt-documents-in-storage.md) | [androidJvm]<br>val [encryptDocumentsInStorage](encrypt-documents-in-storage.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Encrypt documents in storage If true, the documents will be encrypted in the storage. |
| [openId4VpVerifierApiUri](open-id4-vp-verifier-api-uri.md) | [androidJvm]<br>val [openId4VpVerifierApiUri](open-id4-vp-verifier-api-uri.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>OpenId4Vp verifier uri This is the uri of the verifier that will be used to verify using OpenId4Vp. |
| [trustedReaderCertificates](trusted-reader-certificates.md) | [androidJvm]<br>val [trustedReaderCertificates](trusted-reader-certificates.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;?<br>Trusted reader certificates This is the list of trusted reader certificates. If not set, no reader authentication will be performed. |
| [useHardwareToStoreKeys](use-hardware-to-store-keys.md) | [androidJvm]<br>val [useHardwareToStoreKeys](use-hardware-to-store-keys.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Use hardware to store keys If true and supported by device, documents' keys will be stored in the hardware. |
| [userAuthenticationRequired](user-authentication-required.md) | [androidJvm]<br>val [userAuthenticationRequired](user-authentication-required.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>User authentication required If true, the user will be asked to authenticate before accessing the documents' attestations. |
| [userAuthenticationTimeOut](user-authentication-time-out.md) | [androidJvm]<br>val [userAuthenticationTimeOut](user-authentication-time-out.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>User authentication time out This is the time out for the user authentication. |
