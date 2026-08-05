//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[DisclosedDocument](index.md)/[DisclosedDocument](-disclosed-document.md)

# DisclosedDocument

[release]\
constructor(documentId: DocumentId, disclosedItems: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;, keyUnlockData: KeyUnlockData? = null)

[release]\
constructor(requestedDocument: [RequestedDocument](../-requested-document/index.md), disclosedItems: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;? = null, keyUnlockData: KeyUnlockData? = null)

Alternative constructor that takes a [RequestedDocument](../-requested-document/index.md) and a [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of [DocItem](../-doc-item/index.md) to create a [DisclosedDocument](-disclosed-document.md)

#### Parameters

release

| | |
|---|---|
| requestedDocument | the requested document |
| disclosedItems | the list of disclosed items. If not provided, it will be set to the list of requested items |
| keyUnlockData | the key unlock data |