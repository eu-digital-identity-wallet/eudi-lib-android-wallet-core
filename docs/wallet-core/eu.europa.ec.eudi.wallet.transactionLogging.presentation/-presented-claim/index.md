//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[PresentedClaim](index.md)

# PresentedClaim

[release]\
data class [PresentedClaim](index.md)(val path: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;, val value: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?, val rawValue: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html), val metadata: IssuerMetadata.Claim?)

Data class representing a presented claim in a presentation transaction log.

## Constructors

| | |
|---|---|
| [PresentedClaim](-presented-claim.md) | [release]<br>constructor(path: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;, value: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?, rawValue: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html), metadata: IssuerMetadata.Claim?) |

## Properties

| Name | Summary |
|---|---|
| [metadata](metadata.md) | [release]<br>val [metadata](metadata.md): IssuerMetadata.Claim?<br>The metadata associated with the claim. |
| [path](path.md) | [release]<br>val [path](path.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>The path to the claim. |
| [rawValue](raw-value.md) | [release]<br>val [rawValue](raw-value.md): [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)<br>The raw value of the claim. |
| [value](value.md) | [release]<br>val [value](value.md): [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?<br>The value of the claim. |