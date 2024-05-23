//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OfferResult](index.md)

# OfferResult

interface [OfferResult](index.md)

The result of an offer operation.

#### Inheritors

| |
|---|
| [Success](-success/index.md) |
| [Failure](-failure/index.md) |

## Types

| Name | Summary |
|---|---|
| [Failure](-failure/index.md) | [androidJvm]<br>data class [Failure](-failure/index.md)(val error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [OfferResult](index.md)<br>The offer failed. |
| [Success](-success/index.md) | [androidJvm]<br>data class [Success](-success/index.md)(val offer: [Offer](../-offer/index.md)) : [OfferResult](index.md)<br>The offer was successful. |
