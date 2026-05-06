//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[OnResolvedOffer](index.md)

# OnResolvedOffer

[release]\
fun interface [OnResolvedOffer](index.md) : [OpenId4VciManager.OnResult](../-on-result/index.md)&lt;[OfferResult](../../-offer-result/index.md)&gt; 

Callback to be called when an offer is resolved

## Functions

| Name | Summary |
|---|---|
| [invoke](../-on-result/invoke.md) | [release]<br>open operator fun [invoke](../-on-result/invoke.md)(result: [OfferResult](../../-offer-result/index.md)) |
| [onResult](../-on-result/on-result.md) | [release]<br>abstract fun [onResult](../-on-result/on-result.md)(result: [OfferResult](../../-offer-result/index.md)) |