//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)

# EudiWalletConfig

class [EudiWalletConfig](index.md)

Eudi wallet config. This config is used to configure the default settings of the Eudi wallet.

Custom configuration and implementations of the various components can be provided using the [EudiWallet.Builder](../-eudi-wallet/-builder/index.md) class.

Example usage:

```kotlin
val config = EudiWalletConfig()
    .configureDocumentManager(context.noBackupFilesDir)
    .configureLogging(
        // set log level to info
        level = Logger.LEVEL_INFO
    )
    .configureDocumentKeyCreation(
        // set userAuthenticationRequired to true to require user authentication
        userAuthenticationRequired = true,
        // set userAuthenticationTimeout to 30 seconds
        userAuthenticationTimeout = 30_000L,
        // set useStrongBoxForKeys to true to use the the device's StrongBox if available
        // to store the keys
        useStrongBoxForKeys = true
    )
    .configureReaderTrustStore(
        // set the reader trusted certificates for the reader trust store
        listOf(readerCertificate)
    )
    .configureOpenId4Vci {
        withIssuerUrl("https://issuer.com")
        withClientId("client-id")
        withAuthFlowRedirectionURI("eudi-openid4ci://authorize")
        withParUsage(OpenId4VciManager.Config.ParUsage.Companion.IF_SUPPORTED)
        withUseDPoPIfSupported(true)
    }
    .configureProximityPresentation(
        enableBlePeripheralMode = true,
        enableBleCentralMode = false,
        clearBleCache = true,
    )
    .configureOpenId4Vp {
        withEncryptionAlgorithms(
            EncryptionAlgorithm.ECDH_ES
        )
        withEncryptionMethods(
            EncryptionMethod.A128CBC_HS256,
            EncryptionMethod.A256GCM
        )
        withClientIdSchemes(
            ClientIdScheme.X509SanDns
        )
        withSchemes(
            "openid4vp",
            "eudi-openid4vp",
            "mdoc-openid4vp"
        )
    }
```

#### See also

| |
|---|
| [EudiWallet.Builder](../-eudi-wallet/-builder/index.md) |

## Constructors

| | |
|---|---|
| [EudiWalletConfig](-eudi-wallet-config.md) | [androidJvm]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [clearBleCache](clear-ble-cache.md) | [androidJvm]<br>var [clearBleCache](clear-ble-cache.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>whether to clear the BLE cache |
| [documentManagerIdentifier](document-manager-identifier.md) | [androidJvm]<br>var [documentManagerIdentifier](document-manager-identifier.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the document manager identifier |
| [documentsStorageDir](documents-storage-dir.md) | [androidJvm]<br>var [documentsStorageDir](documents-storage-dir.md): [File](https://developer.android.com/reference/kotlin/java/io/File.html)?<br>the documents storage directory |
| [enableBleCentralMode](enable-ble-central-mode.md) | [androidJvm]<br>var [enableBleCentralMode](enable-ble-central-mode.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>whether to enable BLE central mode |
| [enableBlePeripheralMode](enable-ble-peripheral-mode.md) | [androidJvm]<br>var [enableBlePeripheralMode](enable-ble-peripheral-mode.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>whether to enable BLE peripheral mode |
| [encryptDocumentsInStorage](encrypt-documents-in-storage.md) | [androidJvm]<br>var [encryptDocumentsInStorage](encrypt-documents-in-storage.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>whether to encrypt documents in storage |
| [logLevel](log-level.md) | [androidJvm]<br>var [logLevel](log-level.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>the log level |
| [logSizeLimit](log-size-limit.md) | [androidJvm]<br>var [logSizeLimit](log-size-limit.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>the log size limit |
| [nfcEngagementServiceClass](nfc-engagement-service-class.md) | [androidJvm]<br>var [nfcEngagementServiceClass](nfc-engagement-service-class.md): [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? |
| [openId4VciConfig](open-id4-vci-config.md) | [androidJvm]<br>var [openId4VciConfig](open-id4-vci-config.md): [OpenId4VciManager.Config](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/index.md)?<br>the OpenID4VCI configuration |
| [openId4VpConfig](open-id4-vp-config.md) | [androidJvm]<br>var [openId4VpConfig](open-id4-vp-config.md): [OpenId4VpConfig](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-config/index.md)?<br>the OpenID4VP configuration |
| [readerTrustedCertificates](reader-trusted-certificates.md) | [androidJvm]<br>var [readerTrustedCertificates](reader-trusted-certificates.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;?<br>the reader trusted certificates |
| [userAuthenticationRequired](user-authentication-required.md) | [androidJvm]<br>var [userAuthenticationRequired](user-authentication-required.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>whether user authentication is required |
| [userAuthenticationTimeout](user-authentication-timeout.md) | [androidJvm]<br>var [userAuthenticationTimeout](user-authentication-timeout.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>the user authentication timeout |
| [useStrongBoxForKeys](use-strong-box-for-keys.md) | [androidJvm]<br>var [useStrongBoxForKeys](use-strong-box-for-keys.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>whether to use the strong box for keys |

## Functions

| Name | Summary |
|---|---|
| [configureDocumentKeyCreation](configure-document-key-creation.md) | [androidJvm]<br>fun [configureDocumentKeyCreation](configure-document-key-creation.md)(userAuthenticationRequired: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, userAuthenticationTimeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) = 0, useStrongBoxForKeys: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true): [EudiWalletConfig](index.md)<br>Configure the document key creation. This allows to configure if user authentication is required to unlock key usage, the user authentication timeout and whether to use the strong box for keys. These values are used to create the eu.europa.ec.eudi.wallet.document.CreateDocumentSettings using [eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings](../../eu.europa.ec.eudi.wallet.document/-document-extensions/get-default-create-document-settings.md) method. |
| [configureDocumentManager](configure-document-manager.md) | [androidJvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [configureDocumentManager](configure-document-manager.md)(storageDir: [File](https://developer.android.com/reference/kotlin/java/io/File.html), identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, encryptDocuments: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true): [EudiWalletConfig](index.md)<br>Configure the built-in document manager. |
| [configureLogging](configure-logging.md) | [androidJvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [configureLogging](configure-logging.md)(level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), sizeLimit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = null): [EudiWalletConfig](index.md)<br>Configure the built-in logging. This allows to configure the log level and the log size limit. |
| [configureOpenId4Vci](configure-open-id4-vci.md) | [androidJvm]<br>fun [configureOpenId4Vci](configure-open-id4-vci.md)(openId4VciConfig: [OpenId4VciManager.Config](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/index.md)): [EudiWalletConfig](index.md)<br>Configure OpenID4VCI.<br>[androidJvm]<br>fun [configureOpenId4Vci](configure-open-id4-vci.md)(openId4VciConfig: [OpenId4VciManager.Config.Builder](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [EudiWalletConfig](index.md)<br>Configure OpenID4VCI using a [OpenId4VciManager.Config.Builder](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/-builder/index.md) as a lambda with receiver. |
| [configureOpenId4Vp](configure-open-id4-vp.md) | [androidJvm]<br>fun [configureOpenId4Vp](configure-open-id4-vp.md)(openId4VpConfig: [OpenId4VpConfig](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-config/index.md)): [EudiWalletConfig](index.md)<br>Configure OpenID4VP.<br>[androidJvm]<br>fun [configureOpenId4Vp](configure-open-id4-vp.md)(openId4VpConfig: [OpenId4VpConfig.Builder](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-config/-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [EudiWalletConfig](index.md)<br>Configure OpenID4VP using a [OpenId4VpConfig.Builder](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-config/-builder/index.md) as a lambda with receiver. |
| [configureProximityPresentation](configure-proximity-presentation.md) | [androidJvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [configureProximityPresentation](configure-proximity-presentation.md)(enableBlePeripheralMode: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, enableBleCentralMode: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, clearBleCache: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, nfcEngagementServiceClass: [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? = null): [EudiWalletConfig](index.md)<br>Configure the proximity presentation. This allows to configure the BLE peripheral mode, the BLE central mode and whether to clear the BLE cache. Also, it allows to set the NFC engagement service class an implementation of NfcEngagementService, which is used to handle the NFC engagement. |
| [configureReaderTrustStore](configure-reader-trust-store.md) | [androidJvm]<br>fun [configureReaderTrustStore](configure-reader-trust-store.md)(vararg readerTrustedCertificates: [X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)): [EudiWalletConfig](index.md)<br>fun [configureReaderTrustStore](configure-reader-trust-store.md)(readerTrustedCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [EudiWalletConfig](index.md)<br>Configure the built-in ReaderTrustStore. This allows to set the reader trusted certificates for the reader trust store.<br>[androidJvm]<br>fun [configureReaderTrustStore](configure-reader-trust-store.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), @[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg certificateRes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EudiWalletConfig](index.md)<br>Configure the built-in ReaderTrustStore. This allows to set the reader trusted certificates for the reader trust store. The certificates are loaded from the raw resources. |
