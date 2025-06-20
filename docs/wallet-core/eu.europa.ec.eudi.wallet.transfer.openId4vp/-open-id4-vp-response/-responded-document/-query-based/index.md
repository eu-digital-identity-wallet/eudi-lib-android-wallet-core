//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../../index.md)/[OpenId4VpResponse](../../index.md)/[RespondedDocument](../index.md)/[QueryBased](index.md)

# QueryBased

[androidJvm]\
@Serializable

data class [QueryBased](index.md)(val documentId: DocumentId, val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val queryId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [OpenId4VpResponse.RespondedDocument](../index.md)

Query-based representation of a responded document.

Used when documents are associated with specific query IDs, typically for DCQL-based responses.

## Constructors

| | |
|---|---|
| [QueryBased](-query-based.md) | [androidJvm]<br>constructor(documentId: DocumentId, format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), queryId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [documentId](document-id.md) | [androidJvm]<br>open override val [documentId](document-id.md): DocumentId<br>The identifier of the document |
| [format](format.md) | [androidJvm]<br>open override val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The format of the document |
| [queryId](query-id.md) | [androidJvm]<br>val [queryId](query-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The query identifier this document responds to |
