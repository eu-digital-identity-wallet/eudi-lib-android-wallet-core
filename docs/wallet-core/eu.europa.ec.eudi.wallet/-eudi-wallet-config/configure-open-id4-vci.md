//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureOpenId4Vci](configure-open-id4-vci.md)

# configureOpenId4Vci

[release]\
fun [configureOpenId4Vci](configure-open-id4-vci.md)(openId4VciConfig: [OpenId4VciManager.Config](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/index.md)): [EudiWalletConfig](index.md)

Configure OpenID for Verifiable Credential Issuance (OpenID4VCI). This configuration is used by [EudiWallet.createOpenId4VciManager](../-eudi-wallet/create-open-id4-vci-manager.md) when no specific config is provided.

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

release

| | |
|---|---|
| openId4VciConfig | the OpenID4VCI configuration |

#### See also

| |
|---|
| [OpenId4VciManager.Config](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/index.md) |
| [OpenId4VciManager.Config.Builder](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/-builder/index.md) |

[release]\
fun [configureOpenId4Vci](configure-open-id4-vci.md)(openId4VciConfig: [OpenId4VciManager.Config.Builder](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)): [EudiWalletConfig](index.md)

Configure OpenID for Verifiable Credential Issuance (OpenID4VCI) using a builder pattern. This configuration is used by [EudiWallet.createOpenId4VciManager](../-eudi-wallet/create-open-id4-vci-manager.md) when no specific config is provided.

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

release

| | |
|---|---|
| openId4VciConfig | the OpenID4VCI configuration lambda with [OpenId4VciManager.Config.Builder](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/-builder/index.md) as receiver |

#### See also

| |
|---|
| [OpenId4VciManager.Config](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/index.md) |
| [OpenId4VciManager.Config.Builder](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-config/-builder/index.md) |