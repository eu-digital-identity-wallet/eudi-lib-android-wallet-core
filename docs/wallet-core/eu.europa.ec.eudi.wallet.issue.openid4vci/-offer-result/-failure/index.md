//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OfferResult](../index.md)/[Failure](index.md)

# Failure

[androidJvm]\
data class [Failure](index.md)(val cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)) : [OfferResult](../index.md), [OpenId4VciResult.Erroneous](../../-open-id4-vci-result/-erroneous/index.md)

The offer failed.

## Constructors

| | |
|---|---|
| [Failure](-failure.md) | [androidJvm]<br>constructor(cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [cause](cause.md) | [androidJvm]<br>open override val [cause](cause.md): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)<br>the error that caused the failure |
