//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManager](index.md)/[storeIssuedDocument](store-issued-document.md)

# storeIssuedDocument

[release]\
abstract fun [storeIssuedDocument](store-issued-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), issuerProvidedData: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerProvidedCredential](../../eu.europa.ec.eudi.wallet.document.credential/-issuer-provided-credential/index.md)&gt;): [Outcome](../-outcome/index.md)&lt;[IssuedDocument](../-issued-document/index.md)&gt;

Stores a document that has completed the issuance process with an issuer.

This method finalizes the document issuance process by storing the document along with the issuer-provided credentials. It completes the document lifecycle from unsigned to fully issued status. The document becomes ready for use in verification scenarios.

The method performs the following key operations:

1. 
   Validates that the issuer-provided credentials match the document's pending credentials
2. 
   Certifies each credential using the appropriate credential certification handler
3. 
   Updates the document metadata (name, issuance timestamp)
4. 
   Clears any deferred issuance data that may exist

#### Return

An [Outcome](../-outcome/index.md) containing either:          - A success result with the stored [IssuedDocument](../-issued-document/index.md)          - A failure result with an exception describing what went wrong

#### Parameters

release

| | |
|---|---|
| unsignedDocument | The unsigned document to be transformed into an issued document |
| issuerProvidedData | List of credentials provided by the issuer containing the certified claims |