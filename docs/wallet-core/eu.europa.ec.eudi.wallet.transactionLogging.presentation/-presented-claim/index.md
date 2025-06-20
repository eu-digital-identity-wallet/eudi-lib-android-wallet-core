//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[PresentedClaim](index.md)

# PresentedClaim

[androidJvm]\
data class [PresentedClaim](index.md)(val path: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?, val rawValue: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), val metadata: IssuerMetadata.Claim?)

Data class representing a presented claim in a presentation transaction log.

## Constructors

| | |
|---|---|
| [PresentedClaim](-presented-claim.md) | [androidJvm]<br>constructor(path: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?, rawValue: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), metadata: IssuerMetadata.Claim?) |

## Properties

| Name | Summary |
|---|---|
| [metadata](metadata.md) | [androidJvm]<br>val [metadata](metadata.md): IssuerMetadata.Claim?<br>The metadata associated with the claim. |
| [path](path.md) | [androidJvm]<br>val [path](path.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>The path to the claim. |
| [rawValue](raw-value.md) | [androidJvm]<br>val [rawValue](raw-value.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)<br>The raw issuerMetadata of the claim. |
| [value](value.md) | [androidJvm]<br>val [value](value.md): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?<br>The issuerMetadata of the claim. |
