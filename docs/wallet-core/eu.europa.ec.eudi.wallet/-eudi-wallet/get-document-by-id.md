//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[getDocumentById](get-document-by-id.md)

# getDocumentById

[androidJvm]\
fun [getDocumentById](get-document-by-id.md)(documentId: DocumentId): Document?

Returns the document with the given [documentId](get-document-by-id.md)

#### Return

the document with the given [documentId](get-document-by-id.md) or null if not found

#### Parameters

androidJvm

| | |
|---|---|
| documentId | the document's id |

#### See also

| |
|---|
| DocumentManager.getDocumentById |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
