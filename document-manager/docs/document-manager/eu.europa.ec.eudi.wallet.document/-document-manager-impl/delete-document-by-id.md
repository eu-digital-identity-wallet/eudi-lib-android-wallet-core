//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManagerImpl](index.md)/[deleteDocumentById](delete-document-by-id.md)

# deleteDocumentById

[release]\
open override fun [deleteDocumentById](delete-document-by-id.md)(documentId: [DocumentId](../-document-id/index.md)): [Outcome](../-outcome/index.md)&lt;[ProofOfDeletion](../-proof-of-deletion/index.md)?&gt;

Delete a document by its identifier.

#### Return

the result of the deletion. If successful, it will return a proof of deletion. If not, it will return an error.

#### Parameters

release

| | |
|---|---|
| documentId | the identifier of the document |