//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../../../index.md)/[TransactionLog](../../index.md)/[Metadata](../index.md)/[QueryBased](index.md)

# QueryBased

[androidJvm]\
@Serializable

data class [QueryBased](index.md)(val queryId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val issuerMetadata: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) : [TransactionLog.Metadata](../index.md)

## Constructors

| | |
|---|---|
| [QueryBased](-query-based.md) | [androidJvm]<br>constructor(queryId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), issuerMetadata: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) |

## Properties

| Name | Summary |
|---|---|
| [format](format.md) | [androidJvm]<br>open override val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [issuerMetadata](issuer-metadata.md) | [androidJvm]<br>open override val [issuerMetadata](issuer-metadata.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? |
| [queryId](query-id.md) | [androidJvm]<br>val [queryId](query-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |

## Functions

| Name | Summary |
|---|---|
| [toJson](../to-json.md) | [androidJvm]<br>open fun [toJson](../to-json.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [toString](to-string.md) | [androidJvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
