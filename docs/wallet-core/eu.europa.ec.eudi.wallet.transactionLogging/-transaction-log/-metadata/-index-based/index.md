//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../../../index.md)/[TransactionLog](../../index.md)/[Metadata](../index.md)/[IndexBased](index.md)

# IndexBased

[androidJvm]\
@Serializable

data class [IndexBased](index.md)(val index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val issuerMetadata: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) : [TransactionLog.Metadata](../index.md)

## Constructors

| | |
|---|---|
| [IndexBased](-index-based.md) | [androidJvm]<br>constructor(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), issuerMetadata: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) |

## Properties

| Name | Summary |
|---|---|
| [format](format.md) | [androidJvm]<br>open override val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [index](--index--.md) | [androidJvm]<br>val [index](--index--.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [issuerMetadata](issuer-metadata.md) | [androidJvm]<br>open override val [issuerMetadata](issuer-metadata.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? |

## Functions

| Name | Summary |
|---|---|
| [toJson](../to-json.md) | [androidJvm]<br>open fun [toJson](../to-json.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [toString](to-string.md) | [androidJvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
