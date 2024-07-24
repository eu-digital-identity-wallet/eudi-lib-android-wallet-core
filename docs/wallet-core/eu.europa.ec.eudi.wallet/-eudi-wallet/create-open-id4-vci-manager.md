//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[createOpenId4VciManager](create-open-id4-vci-manager.md)

# createOpenId4VciManager

[androidJvm]\
fun [createOpenId4VciManager](create-open-id4-vci-manager.md)(): [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md)

Creates and returns
an [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) instance

#### Return

[OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md)

#### See also

|                                                                                                     |
|-----------------------------------------------------------------------------------------------------|
| [OpenId4VciManager](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) |

#### Throws

|                                                                                                                  |                                                                                                                                                                                             |
|------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method or if the [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md) is not set |
