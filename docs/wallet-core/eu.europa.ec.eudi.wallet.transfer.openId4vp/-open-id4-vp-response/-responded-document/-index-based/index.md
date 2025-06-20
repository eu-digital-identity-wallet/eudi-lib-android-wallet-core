//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../../index.md)/[OpenId4VpResponse](../../index.md)/[RespondedDocument](../index.md)/[IndexBased](index.md)

# IndexBased

[androidJvm]\
@Serializable

data class [IndexBased](index.md)(val documentId: DocumentId, val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) : [OpenId4VpResponse.RespondedDocument](../index.md)

Index-based representation of a responded document.

Used when documents are positioned by index in the response, typically for device-based or presentation exchange responses.

## Constructors

| | |
|---|---|
| [IndexBased](-index-based.md) | [androidJvm]<br>constructor(documentId: DocumentId, format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [documentId](document-id.md) | [androidJvm]<br>open override val [documentId](document-id.md): DocumentId<br>The identifier of the document |
| [format](format.md) | [androidJvm]<br>open override val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The format of the document |
| [index](--index--.md) | [androidJvm]<br>val [index](--index--.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The position of this document in the response array |
