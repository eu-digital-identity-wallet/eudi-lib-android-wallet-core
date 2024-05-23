//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[resolveDocumentOffer](resolve-document-offer.md)

# resolveDocumentOffer

[androidJvm]\
fun [resolveDocumentOffer](resolve-document-offer.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onResult: [OpenId4VciManager.OnResolvedOffer](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-resolved-offer/index.md))

Resolves a document offer using OpenId4VCI protocol

#### Parameters

androidJvm

| | |
|---|---|
| offerUri | the offer uri |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onResult | the callback to be called when the offer is resolved |

#### See also

| | |
|---|---|
| [OpenId4VciManager.OnResolvedOffer](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-resolved-offer/index.md) | on how to handle the result |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md) is not set |
