//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletImpl](index.md)/[createOpenId4VciManager](create-open-id4-vci-manager.md)

# createOpenId4VciManager

[androidJvm]\
open override fun [createOpenId4VciManager](create-open-id4-vci-manager.md)(config: [OpenId4VciManager.Config](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/index.md)?, ktorHttpClientFactory: () -&gt; HttpClient?): [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md)

Creates an instance of [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) for interacting with the OpenID for Verifiable Credential Issuance protocol.

The configuration can be provided in two ways:

1. 
   As a parameter to this method
2. 
   From the wallet's [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md)

#### Return

An instance of [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) configured with the provided or default settings

#### Parameters

androidJvm

| | |
|---|---|
| config | Optional configuration for the OpenId4VciManager. If null, the configuration from [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md)     will be used. If both are null, an [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) is thrown. |
| ktorHttpClientFactory | Optional HTTP client factory to use for network requests. If null, the wallet's     configured HTTP client factory will be used. |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | If neither a config parameter is provided nor a configuration exists in [EudiWalletConfig](../-eudi-wallet-config/index.md) |
