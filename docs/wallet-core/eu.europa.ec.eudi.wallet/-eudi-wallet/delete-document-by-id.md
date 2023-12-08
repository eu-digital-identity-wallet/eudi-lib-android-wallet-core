//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[deleteDocumentById](delete-document-by-id.md)

# deleteDocumentById

[androidJvm]\
fun [deleteDocumentById](delete-document-by-id.md)(documentId: DocumentId): DeleteDocumentResult

Delete the document with the given [documentId](delete-document-by-id.md)

#### Return

DeleteDocumentResult

#### Parameters

androidJvm

|            |                   |
|------------|-------------------|
| documentId | the document's id |

#### See also

|                                    |
|------------------------------------|
| DocumentManager.deleteDocumentById |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
