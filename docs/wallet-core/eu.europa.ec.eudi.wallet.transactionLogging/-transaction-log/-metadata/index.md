//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../../index.md)/[TransactionLog](../index.md)/[Metadata](index.md)

# Metadata

@Serializable

sealed interface [Metadata](index.md)

#### Inheritors

| |
|---|
| [IndexBased](-index-based/index.md) |
| [QueryBased](-query-based/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |
| [IndexBased](-index-based/index.md) | [androidJvm]<br>@Serializable<br>data class [IndexBased](-index-based/index.md)(val index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val issuerMetadata: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) : [TransactionLog.Metadata](index.md) |
| [QueryBased](-query-based/index.md) | [androidJvm]<br>@Serializable<br>data class [QueryBased](-query-based/index.md)(val queryId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val issuerMetadata: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) : [TransactionLog.Metadata](index.md) |

## Properties

| Name | Summary |
|---|---|
| [format](format.md) | [androidJvm]<br>abstract val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [issuerMetadata](issuer-metadata.md) | [androidJvm]<br>abstract val [issuerMetadata](issuer-metadata.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? |

## Functions

| Name | Summary |
|---|---|
| [toJson](to-json.md) | [androidJvm]<br>open fun [toJson](to-json.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
