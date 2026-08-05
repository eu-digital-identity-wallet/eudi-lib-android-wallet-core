//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)

# Builder

class [Builder](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), val config: [EudiWalletConfig](../../-eudi-wallet-config/index.md), val walletProvider: [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md)?)

Builder class to create an instance of [EudiWallet](../index.md)

#### Parameters

release

| | |
|---|---|
| context | application context |
| config | the configuration object |

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), config: [EudiWalletConfig](../../-eudi-wallet-config/index.md), walletProvider: [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md)?) |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [release]<br>val [config](config.md): [EudiWalletConfig](../../-eudi-wallet-config/index.md)<br>the configuration object |
| [dcapiRegistration](dcapi-registration.md) | [release]<br>var [dcapiRegistration](dcapi-registration.md): [DCAPIRegistration](../../../eu.europa.ec.eudi.wallet.dcapi/-d-c-a-p-i-registration/index.md)?<br>the DCAPI registration to use if you want to provide a custom implementation, by default it will be DCAPIIsoMdocRegistration when the DCAPI is enabled in the configuration |
| [documentManager](document-manager.md) | [release]<br>var [documentManager](document-manager.md): DocumentManager?<br>the document manager to use if you want to provide a custom implementation |
| [documentStatusResolver](document-status-resolver.md) | [release]<br>var [documentStatusResolver](document-status-resolver.md): [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md)?<br>the document status resolver to use if you want to provide a custom implementation |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [release]<br>var [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient?<br>the Ktor HTTP client factory to use if you want to provide a custom implementation |
| [logger](logger.md) | [release]<br>var [logger](logger.md): [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>the logger to use if you want to provide a custom implementation |
| [presentationManager](presentation-manager.md) | [release]<br>var [presentationManager](presentation-manager.md): [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)?<br>the presentation manager to use if you want to provide a custom implementation |
| [readerTrustStore](reader-trust-store.md) | [release]<br>var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>the reader trust store to use if you want to provide a custom implementation |
| [secureAreas](secure-areas.md) | [release]<br>var [secureAreas](secure-areas.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureArea&gt;?<br>the secure areas to use for documents' keys management if you want to provide a different implementation |
| [storage](storage.md) | [release]<br>var [storage](storage.md): Storage?<br>the storage to use for storing/retrieving documents if you want to provide a different implementation |
| [transactionLogger](transaction-logger.md) | [release]<br>var [transactionLogger](transaction-logger.md): [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md)?<br>the transaction logger to use if you want to provide a custom implementation |
| [walletKeyManager](wallet-key-manager.md) | [release]<br>var [walletKeyManager](wallet-key-manager.md): [WalletKeyManager](../../../eu.europa.ec.eudi.wallet.provider/-wallet-key-manager/index.md)? |
| [walletProvider](wallet-provider.md) | [release]<br>val [walletProvider](wallet-provider.md): [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md)? |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [EudiWallet](../index.md)<br>Build the [EudiWallet](../index.md) instance |
| [withDCAPIRegistration](with-d-c-a-p-i-registration.md) | [release]<br>fun [withDCAPIRegistration](with-d-c-a-p-i-registration.md)(dcapiRegistration: [DCAPIRegistration](../../../eu.europa.ec.eudi.wallet.dcapi/-d-c-a-p-i-registration/index.md)): [EudiWallet.Builder](index.md)<br>Configure with the given [DCAPIRegistration](../../../eu.europa.ec.eudi.wallet.dcapi/-d-c-a-p-i-registration/index.md) to use for registering credentials with the Digital Credential API (DCAPI). If not set, the default DCAPIIsoMdocRegistration will be used when the DCAPI is enabled in the configuration. |
| [withDocumentManager](with-document-manager.md) | [release]<br>fun [withDocumentManager](with-document-manager.md)(documentManager: DocumentManager): [EudiWallet.Builder](index.md)<br>Configure with the given DocumentManager to use. If not set, the default document manager will be used which is DocumentManagerImpl configured with the provided storageEngine and [secureAreas](secure-areas.md) if they are set. |
| [withDocumentStatusResolver](with-document-status-resolver.md) | [release]<br>fun [withDocumentStatusResolver](with-document-status-resolver.md)(documentStatusResolver: [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md)): [EudiWallet.Builder](index.md)<br>Configure with the given [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md) to use for resolving the status of documents. If not set, the default document status resolver will be used which is [eu.europa.ec.eudi.wallet.statium.DocumentStatusResolverImpl](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver-impl/index.md) that uses the HttpClient provided in the configuration. |
| [withKtorHttpClientFactory](with-ktor-http-client-factory.md) | [release]<br>fun [withKtorHttpClientFactory](with-ktor-http-client-factory.md)(ktorHttpClientFactory: () -&gt; HttpClient): [EudiWallet.Builder](index.md)<br>Configure with the given Ktor HTTP client factory to use for making HTTP requests. Ktor HTTP client is used by the [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) and [OpenId4VciManager](../../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) for making HTTP requests. |
| [withLogger](with-logger.md) | [release]<br>fun [withLogger](with-logger.md)(logger: [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)): [EudiWallet.Builder](index.md)<br>Configure with the given [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md) to use for logging. If not set, the default logger will be used which is configured with the [EudiWalletConfig.configureLogging](../../-eudi-wallet-config/configure-logging.md). |
| [withPresentationManager](with-presentation-manager.md) | [release]<br>fun [withPresentationManager](with-presentation-manager.md)(presentationManager: [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)): [EudiWallet.Builder](index.md)<br>Configure with the given [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) to use for both proximity and remote presentation. If not set, the default presentation manager will be used which is [PresentationManagerImpl](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager-impl/index.md) that uses the eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl for proximity presentation and [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) for remote presentation. |
| [withReaderTrustStore](with-reader-trust-store.md) | [release]<br>fun [withReaderTrustStore](with-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): [EudiWallet.Builder](index.md)<br>Configure with the given ReaderTrustStore to use for performing reader authentication. If not set, the default reader trust store will be used which is initialized with the certificates provided in the [EudiWalletConfig.configureReaderTrustStore](../../-eudi-wallet-config/configure-reader-trust-store.md) methods. |
| [withSecureAreas](with-secure-areas.md) | [release]<br>fun [withSecureAreas](with-secure-areas.md)(secureAreas: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureArea&gt;): [EudiWallet.Builder](index.md)<br>Configure with the given SecureArea implementations to use for documents' keys management. If not set, the default secure area will be used which is AndroidKeystoreSecureArea. |
| [withStorage](with-storage.md) | [release]<br>fun [withStorage](with-storage.md)(storage: Storage): [EudiWallet.Builder](index.md)<br>Configure with the given Storage to use for storing/retrieving documents. If not set, the default storage will be used which is AndroidStorage. |
| [withTransactionLogger](with-transaction-logger.md) | [release]<br>fun [withTransactionLogger](with-transaction-logger.md)(transactionLogger: [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md)): [EudiWallet.Builder](index.md)<br>Configure with the given [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md) to use for logging transactions. If not set, the default transaction logger will be used which logs transactions to the console. |
| [withWalletKeyManager](with-wallet-key-manager.md) | [release]<br>fun [withWalletKeyManager](with-wallet-key-manager.md)(walletKeyManager: [WalletKeyManager](../../../eu.europa.ec.eudi.wallet.provider/-wallet-key-manager/index.md)): [EudiWallet.Builder](index.md) |