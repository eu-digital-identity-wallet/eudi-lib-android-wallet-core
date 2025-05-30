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
| [documentStatusResolver](document-status-resolver.md) | [androidJvm]<br>var [documentStatusResolver](document-status-resolver.md): [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md)?<br>the document status resolver to use if you want to provide a custom implementation |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [androidJvm]<br>var [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient?<br>the Ktor HTTP client factory to use if you want to provide a custom implementation |
| [logger](logger.md) | [androidJvm]<br>var [logger](logger.md): [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>the logger to use if you want to provide a custom implementation |
| [presentationManager](presentation-manager.md) | [androidJvm]<br>var [presentationManager](presentation-manager.md): [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)?<br>the presentation manager to use if you want to provide a custom implementation |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>the reader trust store to use if you want to provide a custom implementation |
| [secureAreas](secure-areas.md) | [androidJvm]<br>var [secureAreas](secure-areas.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureArea&gt;?<br>the secure areas to use for documents' keys management if you want to provide a different implementation |
| [storage](storage.md) | [androidJvm]<br>var [storage](storage.md): Storage?<br>the storage to use for storing/retrieving documents if you want to provide a different implementation |
| [transactionLogger](transaction-logger.md) | [androidJvm]<br>var [transactionLogger](transaction-logger.md): [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md)?<br>the transaction logger to use if you want to provide a custom implementation |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [EudiWallet](../index.md)<br>Build the [EudiWallet](../index.md) instance |
| [withDocumentManager](with-document-manager.md) | [androidJvm]<br>fun [withDocumentManager](with-document-manager.md)(documentManager: DocumentManager): &lt;Error class: unknown class&gt;<br>Configure with the given DocumentManager to use. If not set, the default document manager will be used which is DocumentManagerImpl configured with the provided storageEngine and [secureAreas](secure-areas.md) if they are set. |
| [withDocumentStatusResolver](with-document-status-resolver.md) | [androidJvm]<br>fun [withDocumentStatusResolver](with-document-status-resolver.md)(documentStatusResolver: [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md)): &lt;Error class: unknown class&gt;<br>Configure with the given [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md) to use for resolving the status of documents. If not set, the default document status resolver will be used which is [eu.europa.ec.eudi.wallet.statium.DocumentStatusResolverImpl](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver-impl/index.md) that uses the HttpClient provided in the configuration. |
| [withKtorHttpClientFactory](with-ktor-http-client-factory.md) | [androidJvm]<br>fun [withKtorHttpClientFactory](with-ktor-http-client-factory.md)(ktorHttpClientFactory: () -&gt; HttpClient): &lt;Error class: unknown class&gt;<br>Configure with the given Ktor HTTP client factory to use for making HTTP requests. Ktor HTTP client is used by the [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) and [OpenId4VciManager](../../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) for making HTTP requests. |
| [withLogger](with-logger.md) | [androidJvm]<br>fun [withLogger](with-logger.md)(logger: [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)): &lt;Error class: unknown class&gt;<br>Configure with the given [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md) to use for logging. If not set, the default logger will be used which is configured with the [EudiWalletConfig.configureLogging](../../-eudi-wallet-config/configure-logging.md). |
| [withPresentationManager](with-presentation-manager.md) | [androidJvm]<br>fun [withPresentationManager](with-presentation-manager.md)(presentationManager: [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)): &lt;Error class: unknown class&gt;<br>Configure with the given [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) to use for both proximity and remote presentation. If not set, the default presentation manager will be used which is [PresentationManagerImpl](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager-impl/index.md) that uses the eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl for proximity presentation and [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) for remote presentation. |
| [withReaderTrustStore](with-reader-trust-store.md) | [androidJvm]<br>fun [withReaderTrustStore](with-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): &lt;Error class: unknown class&gt;<br>Configure with the given ReaderTrustStore to use for performing reader authentication. If not set, the default reader trust store will be used which is initialized with the certificates provided in the [EudiWalletConfig.configureReaderTrustStore](../../-eudi-wallet-config/configure-reader-trust-store.md) methods. |
| [withSecureAreas](with-secure-areas.md) | [androidJvm]<br>fun [withSecureAreas](with-secure-areas.md)(secureAreas: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureArea&gt;): &lt;Error class: unknown class&gt;<br>Configure with the given SecureArea implementations to use for documents' keys management. If not set, the default secure area will be used which is AndroidKeystoreSecureArea. |
| [withStorage](with-storage.md) | [androidJvm]<br>fun [withStorage](with-storage.md)(storage: Storage): &lt;Error class: unknown class&gt;<br>Configure with the given Storage to use for storing/retrieving documents. If not set, the default storage will be used which is AndroidStorage. |
| [withTransactionLogger](with-transaction-logger.md) | [androidJvm]<br>fun [withTransactionLogger](with-transaction-logger.md)(transactionLogger: [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md)): &lt;Error class: unknown class&gt;<br>Configure with the given [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md) to use for logging transactions. If not set, the default transaction logger will be used which logs transactions to the console. |
