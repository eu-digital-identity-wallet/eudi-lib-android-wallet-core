//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[PresentedDocument](index.md)

# PresentedDocument

[androidJvm]\
data class [PresentedDocument](index.md)(val format: DocumentFormat, val metadata: IssuerMetadata?, val claims: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedClaim](../-presented-claim/index.md)&gt;)

Data class representing a presented document in a presentation transaction log.

## Constructors

| | |
|---|---|
| [PresentedDocument](-presented-document.md) | [androidJvm]<br>constructor(format: DocumentFormat, metadata: IssuerMetadata?, claims: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedClaim](../-presented-claim/index.md)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [claims](claims.md) | [androidJvm]<br>val [claims](claims.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedClaim](../-presented-claim/index.md)&gt;<br>The list of claims associated with the document. |
| [format](format.md) | [androidJvm]<br>val [format](format.md): DocumentFormat<br>The format of the document. |
| [metadata](metadata.md) | [androidJvm]<br>val [metadata](metadata.md): IssuerMetadata?<br>The metadata associated with the document. |
