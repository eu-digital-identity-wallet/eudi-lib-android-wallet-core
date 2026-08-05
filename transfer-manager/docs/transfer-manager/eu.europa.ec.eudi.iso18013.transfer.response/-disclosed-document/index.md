//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[DisclosedDocument](index.md)

# DisclosedDocument

[release]\
data class [DisclosedDocument](index.md)(val documentId: DocumentId, val disclosedItems: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;, val keyUnlockData: KeyUnlockData? = null)

Represents a response that contains the document data that will be sent to an mdoc verifier

## Constructors

| | |
|---|---|
| [DisclosedDocument](-disclosed-document.md) | [release]<br>constructor(documentId: DocumentId, disclosedItems: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;, keyUnlockData: KeyUnlockData? = null)constructor(requestedDocument: [RequestedDocument](../-requested-document/index.md), disclosedItems: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;? = null, keyUnlockData: KeyUnlockData? = null)<br>Alternative constructor that takes a [RequestedDocument](../-requested-document/index.md) and a [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of [DocItem](../-doc-item/index.md) to create a [DisclosedDocument](-disclosed-document.md) |

## Properties

| Name | Summary |
|---|---|
| [disclosedItems](disclosed-items.md) | [release]<br>val [disclosedItems](disclosed-items.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;<br>a [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) that contains the document items [DocItem](../-doc-item/index.md), i.e the namespaces and the data elements that will be sent in the device response after selective disclosure |
| [documentId](document-id.md) | [release]<br>val [documentId](document-id.md): DocumentId<br>the unique id of the document |
| [keyUnlockData](key-unlock-data.md) | [release]<br>val [keyUnlockData](key-unlock-data.md): KeyUnlockData?<br>the key unlock data that will be used to unlock document's key for signing the response |