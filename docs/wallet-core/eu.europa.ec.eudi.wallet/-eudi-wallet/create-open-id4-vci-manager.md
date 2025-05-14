//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[createOpenId4VciManager](create-open-id4-vci-manager.md)

# createOpenId4VciManager

[androidJvm]\
abstract fun [createOpenId4VciManager](create-open-id4-vci-manager.md)(config: [OpenId4VciManager.Config](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/index.md)? = null, ktorHttpClientFactory: () -&gt; HttpClient? = null): [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md)

Creates an instance of [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) for the wallet to interact with the OpenID for Verifiable Credential Issuance service.

The configuration can be provided in two ways:

1. 
   As a parameter to this method
2. 
   From the wallet's [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md)

#### Return

An instance of [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md)

#### Parameters

androidJvm

| | |
|---|---|
| config | Optional specific configuration for this manager instance. If null, the configuration     from [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md) will be used. |
| ktorHttpClientFactory | Optional HTTP client factory to use for network requests. If null, the     wallet's configured HTTP client factory will be used. |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | If neither a config parameter is provided nor a configuration exists in [EudiWalletConfig](../-eudi-wallet-config/index.md) |
