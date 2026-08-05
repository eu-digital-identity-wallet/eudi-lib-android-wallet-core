//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManagerImpl](index.md)/[createDocument](create-document.md)

# createDocument

[release]\
open override fun [createDocument](create-document.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), createSettings: [CreateDocumentSettings](../-create-document-settings/index.md), issuerMetadata: [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md)? = null): [Outcome](../-outcome/index.md)&lt;[UnsignedDocument](../-unsigned-document/index.md)&gt;

Create a new document. This method will create a new document with the given format and keys settings. If the document is successfully created, it will return an [UnsignedDocument](../-unsigned-document/index.md). This [UnsignedDocument](../-unsigned-document/index.md) contains the keys and the method to proof the ownership of the keys, that can be used with an issuer to retrieve the document's claims. After that the document can be stored using [storeIssuedDocument](store-issued-document.md) or [storeDeferredDocument](store-deferred-document.md).

#### Return

the result of the creation. If successful, it will return the document. If not, it will return an error.

#### Parameters

release

| | |
|---|---|
| format | the format of the document |
| createSettings | the [CreateDocumentSettings](../-create-document-settings/index.md) to use for the new document |
| issuerMetadata | the [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md) data regarding document display |