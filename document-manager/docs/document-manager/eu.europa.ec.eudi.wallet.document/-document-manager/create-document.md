//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManager](index.md)/[createDocument](create-document.md)

# createDocument

[release]\
abstract fun [createDocument](create-document.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), createSettings: [CreateDocumentSettings](../-create-document-settings/index.md), issuerMetadata: [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md)? = null): [Outcome](../-outcome/index.md)&lt;[UnsignedDocument](../-unsigned-document/index.md)&gt;

Creates a new document with the specified format and security settings.

This method initializes a new document with its security infrastructure (keys) according to the provided format and creation settings. The resulting [UnsignedDocument](../-unsigned-document/index.md) contains the necessary keys and means to prove ownership of these keys, which can then be used to interact with an issuer to obtain the document's certified claims.

The document creation workflow typically follows these steps:

1. 
   Create an unsigned document using this method
2. 
   Use the unsigned document in an issuance protocol with a trusted issuer
3. 
   Store the resulting document using either [storeIssuedDocument](store-issued-document.md) or [storeDeferredDocument](store-deferred-document.md)

#### Return

An [Outcome](../-outcome/index.md) containing either:          - A success result with the created [UnsignedDocument](../-unsigned-document/index.md)          - A failure result with an exception describing what went wrong

#### Parameters

release

| | |
|---|---|
| format | The format specification for the document (e.g., MsoMdocFormat, SdJwtVcFormat) |
| createSettings | Configuration for document creation, including security settings and credential policies |
| issuerMetadata | Optional metadata about the issuer, useful for display and verification purposes |