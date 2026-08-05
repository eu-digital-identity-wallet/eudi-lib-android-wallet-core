//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManager](index.md)/[getDocumentById](get-document-by-id.md)

# getDocumentById

[release]\
abstract fun [getDocumentById](get-document-by-id.md)(documentId: [DocumentId](../-document-id/index.md)): [Document](../-document/index.md)?

Retrieves a document by its unique identifier.

This method searches for a document with the specified ID in the document store. It will only return documents that are managed by this DocumentManager instance (matching the DocumentManager's identifier).

#### Return

The document if found and managed by this DocumentManager, null otherwise

#### Parameters

release

| | |
|---|---|
| documentId | The unique identifier of the document to retrieve |