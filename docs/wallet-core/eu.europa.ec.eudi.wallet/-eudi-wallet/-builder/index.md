//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)

# Builder

class [Builder](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), val config: [EudiWalletConfig](../../-eudi-wallet-config/index.md))

Builder class to create an instance of [EudiWallet](../index.md)

#### Parameters

androidJvm

| | |
|---|---|
| context | application context |
| config | the configuration object |

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), config: [EudiWalletConfig](../../-eudi-wallet-config/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [androidJvm]<br>val [config](config.md): [EudiWalletConfig](../../-eudi-wallet-config/index.md)<br>the configuration object |
| [documentManager](document-manager.md) | [androidJvm]<br>var [documentManager](document-manager.md): DocumentManager?<br>the document manager to use if you want to provide a custom implementation |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [androidJvm]<br>var [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient?<br>the Ktor HTTP client factory to use if you want to provide a custom implementation |
| [logger](logger.md) | [androidJvm]<br>var [logger](logger.md): [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>the logger to use if you want to provide a custom implementation |
| [presentationManager](presentation-manager.md) | [androidJvm]<br>var [presentationManager](presentation-manager.md): [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)?<br>the presentation manager to use if you want to provide a custom implementation |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>the reader trust store to use if you want to provide a custom implementation |
| [secureAreas](secure-areas.md) | [androidJvm]<br>var [secureAreas](secure-areas.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureArea&gt;?<br>the secure areas to use for documents' keys management if you want to provide a different implementation |
| [storageEngine](storage-engine.md) | [androidJvm]<br>var [storageEngine](storage-engine.md): StorageEngine?<br>the storage engine to use for storing/retrieving documents if you want to provide a different implementation |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [EudiWallet](../index.md)<br>Build the [EudiWallet](../index.md) instance |
| [withDocumentManager](with-document-manager.md) | [androidJvm]<br>fun [withDocumentManager](with-document-manager.md)(documentManager: DocumentManager): &lt;Error class: unknown class&gt;<br>Configure with the given DocumentManager to use. If not set, the default document manager will be used which is DocumentManagerImpl configured with the provided [storageEngine](storage-engine.md) and [secureAreas](secure-areas.md) if they are set. |
| [withKtorHttpClientFactory](with-ktor-http-client-factory.md) | [androidJvm]<br>fun [withKtorHttpClientFactory](with-ktor-http-client-factory.md)(ktorHttpClientFactory: () -&gt; HttpClient): &lt;Error class: unknown class&gt;<br>Configure with the given Ktor HTTP client factory to use for making HTTP requests. Ktor HTTP client is used by the [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) and [OpenId4VciManager](../../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) for making HTTP requests. |
| [withLogger](with-logger.md) | [androidJvm]<br>fun [withLogger](with-logger.md)(logger: [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)): &lt;Error class: unknown class&gt;<br>Configure with the given [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md) to use for logging. If not set, the default logger will be used which is configured with the [EudiWalletConfig.configureLogging](../../-eudi-wallet-config/configure-logging.md). |
| [withPresentationManager](with-presentation-manager.md) | [androidJvm]<br>fun [withPresentationManager](with-presentation-manager.md)(presentationManager: [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)): &lt;Error class: unknown class&gt;<br>Configure with the given [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) to use for both proximity and remote presentation. If not set, the default presentation manager will be used which is [PresentationManagerImpl](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager-impl/index.md) that uses the eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl for proximity presentation and [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) for remote presentation. |
| [withReaderTrustStore](with-reader-trust-store.md) | [androidJvm]<br>fun [withReaderTrustStore](with-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): &lt;Error class: unknown class&gt;<br>Configure with the given ReaderTrustStore to use for performing reader authentication. If not set, the default reader trust store will be used which is initialized with the certificates provided in the [EudiWalletConfig.configureReaderTrustStore](../../-eudi-wallet-config/configure-reader-trust-store.md) methods. |
| [withSecureAreas](with-secure-areas.md) | [androidJvm]<br>fun [withSecureAreas](with-secure-areas.md)(secureAreas: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureArea&gt;): &lt;Error class: unknown class&gt;<br>Configure with the given SecureArea implementations to use for documents' keys management. If not set, the default secure area will be used which is AndroidKeystoreSecureArea. |
| [withStorageEngine](with-storage-engine.md) | [androidJvm]<br>fun [withStorageEngine](with-storage-engine.md)(storageEngine: StorageEngine): &lt;Error class: unknown class&gt;<br>Configure with the given StorageEngine to use for storing/retrieving documents. If not set, the default storage engine will be used which is AndroidStorageEngine. |
