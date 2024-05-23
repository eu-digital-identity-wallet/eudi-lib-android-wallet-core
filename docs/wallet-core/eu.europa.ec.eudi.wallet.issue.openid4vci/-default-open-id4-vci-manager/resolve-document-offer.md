//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DefaultOpenId4VciManager](index.md)/[resolveDocumentOffer](resolve-document-offer.md)

# resolveDocumentOffer

[androidJvm]\
open override fun [resolveDocumentOffer](resolve-document-offer.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?, onResolvedOffer: [OpenId4VciManager.OnResolvedOffer](../-open-id4-vci-manager/-on-resolved-offer/index.md))

Resolve an offer using OpenId4Vci protocol

#### Parameters

androidJvm

| | |
|---|---|
| offerUri | the offer URI |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onResolvedOffer | the callback to be called when the offer is resolved |
