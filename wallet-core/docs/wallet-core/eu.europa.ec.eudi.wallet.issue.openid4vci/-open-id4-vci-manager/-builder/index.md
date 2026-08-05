//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[Builder](index.md)

# Builder

class [Builder](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))

Builder to create an instance of [OpenId4VciManager](../index.md)

#### Parameters

release

| | |
|---|---|
| context | the context |

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [release]<br>var [config](config.md): [OpenId4VciManager.Config](../-config/index.md)?<br>the [Config](../-config/index.md) to use |
| [documentManager](document-manager.md) | [release]<br>var [documentManager](document-manager.md): DocumentManager?<br>the DocumentManager to use |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [release]<br>var [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient?<br>the factory to create the Ktor HTTP client |
| [logger](logger.md) | [release]<br>var [logger](logger.md): [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>the logger to use |
| [walletAttestationsProvider](wallet-attestations-provider.md) | [release]<br>var [walletAttestationsProvider](wallet-attestations-provider.md): [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md)?<br>the [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md) to use requires user authentication |
| [walletKeyManager](wallet-key-manager.md) | [release]<br>var [walletKeyManager](wallet-key-manager.md): [WalletKeyManager](../../../eu.europa.ec.eudi.wallet.provider/-wallet-key-manager/index.md)?<br>the [WalletKeyManager](../../../eu.europa.ec.eudi.wallet.provider/-wallet-key-manager/index.md) to use |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [OpenId4VciManager](../index.md)<br>Build the [OpenId4VciManager](../index.md) |
| [config](config.md) | [release]<br>fun [config](config.md)(config: [OpenId4VciManager.Config](../-config/index.md)): [OpenId4VciManager.Builder](index.md)<br>Set the [Config](../-config/index.md) to use |
| [documentManager](document-manager.md) | [release]<br>fun [documentManager](document-manager.md)(documentManager: DocumentManager): [OpenId4VciManager.Builder](index.md)<br>Set the DocumentManager to use |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [release]<br>fun [ktorHttpClientFactory](ktor-http-client-factory.md)(factory: () -&gt; HttpClient): [OpenId4VciManager.Builder](index.md)<br>Override the Ktor HTTP client factory |
| [logger](logger.md) | [release]<br>fun [logger](logger.md)(logger: [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)): [OpenId4VciManager.Builder](index.md)<br>Set the logger to use |
| [walletAttestationsProvider](wallet-attestations-provider.md) | [release]<br>fun [walletAttestationsProvider](wallet-attestations-provider.md)(provider: [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md)): [OpenId4VciManager.Builder](index.md)<br>Configures the [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md) |
| [walletKeyManager](wallet-key-manager.md) | [release]<br>fun [walletKeyManager](wallet-key-manager.md)(keyManager: [WalletKeyManager](../../../eu.europa.ec.eudi.wallet.provider/-wallet-key-manager/index.md)): [OpenId4VciManager.Builder](index.md)<br>Configures the [WalletKeyManager](../../../eu.europa.ec.eudi.wallet.provider/-wallet-key-manager/index.md) responsible for managing cryptographic keys. |