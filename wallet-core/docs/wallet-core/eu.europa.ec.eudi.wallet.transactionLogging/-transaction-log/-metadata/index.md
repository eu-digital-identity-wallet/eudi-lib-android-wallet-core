//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../../index.md)/[TransactionLog](../index.md)/[Metadata](index.md)

# Metadata

[release]\
@Serializable

data class [Metadata](index.md)(val issuerMetadata: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?, val format: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val index: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html), val queryId: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null)

## Constructors

| | |
|---|---|
| [Metadata](-metadata.md) | [release]<br>constructor(issuerMetadata: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?, format: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), index: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html), queryId: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [format](format.md) | [release]<br>val [format](format.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [index](--index--.md) | [release]<br>val [index](--index--.md): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |
| [issuerMetadata](issuer-metadata.md) | [release]<br>val [issuerMetadata](issuer-metadata.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? |
| [queryId](query-id.md) | [release]<br>val [queryId](query-id.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? |

## Functions

| Name | Summary |
|---|---|
| [toJson](to-json.md) | [release]<br>fun [toJson](to-json.md)(): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [toString](to-string.md) | [release]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |