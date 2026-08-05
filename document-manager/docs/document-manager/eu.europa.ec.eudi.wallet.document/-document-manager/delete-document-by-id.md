//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManager](index.md)/[deleteDocumentById](delete-document-by-id.md)

# deleteDocumentById

[release]\
abstract fun [deleteDocumentById](delete-document-by-id.md)(documentId: [DocumentId](../-document-id/index.md)): [Outcome](../-outcome/index.md)&lt;[ProofOfDeletion](../-proof-of-deletion/index.md)?&gt;

Deletes a document by its unique identifier.

This method attempts to delete a document with the specified ID from the document store. The document will only be deleted if it's managed by this DocumentManager instance. In some cases, a proof of deletion may be returned upon successful deletion.

#### Return

An [Outcome](../-outcome/index.md) containing either:          - A success result with an optional [ProofOfDeletion](../-proof-of-deletion/index.md) object          - A failure result with an exception (typically [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) if document not found)

#### Parameters

release

| | |
|---|---|
| documentId | The unique identifier of the document to delete |