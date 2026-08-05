//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[RequestedDocument](index.md)

# RequestedDocument

[release]\
data class [RequestedDocument](index.md)(val documentId: DocumentId, val requestedItems: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;out [DocItem](../-doc-item/index.md), [IntentToRetain](../../eu.europa.ec.eudi.iso18013.transfer/-intent-to-retain/index.md)&gt;, val readerAuth: [ReaderAuth](../-reader-auth/index.md)?, zkRequestSystemSpecs: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ZkSystemSpec&gt;? = null)

Represents a request received by a verifier and contains the requested documents and elements

## Constructors

| | |
|---|---|
| [RequestedDocument](-requested-document.md) | [release]<br>constructor(documentId: DocumentId, requestedItems: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;out [DocItem](../-doc-item/index.md), [IntentToRetain](../../eu.europa.ec.eudi.iso18013.transfer/-intent-to-retain/index.md)&gt;, readerAuth: [ReaderAuth](../-reader-auth/index.md)?, zkRequestSystemSpecs: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ZkSystemSpec&gt;? = null) |

## Properties

| Name | Summary |
|---|---|
| [documentId](document-id.md) | [release]<br>val [documentId](document-id.md): DocumentId<br>the unique id of the document |
| [readerAuth](reader-auth.md) | [release]<br>val [readerAuth](reader-auth.md): [ReaderAuth](../-reader-auth/index.md)?<br>the result of the reader authentication |
| [requestedItems](requested-items.md) | [release]<br>val [requestedItems](requested-items.md): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;out [DocItem](../-doc-item/index.md), [IntentToRetain](../../eu.europa.ec.eudi.iso18013.transfer/-intent-to-retain/index.md)&gt;<br>the list of requested items |