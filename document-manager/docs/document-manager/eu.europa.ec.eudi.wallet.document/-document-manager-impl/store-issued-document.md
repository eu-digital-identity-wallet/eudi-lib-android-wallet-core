//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManagerImpl](index.md)/[storeIssuedDocument](store-issued-document.md)

# storeIssuedDocument

[release]\
open override fun [storeIssuedDocument](store-issued-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), issuerProvidedData: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerProvidedCredential](../../eu.europa.ec.eudi.wallet.document.credential/-issuer-provided-credential/index.md)&gt;): [Outcome](../-outcome/index.md)&lt;[IssuedDocument](../-issued-document/index.md)&gt;

Store an issued document. This method will store the document with its issuer provided data.

#### Return

the result of the storage. If successful, it will return the [IssuedDocument](../-issued-document/index.md). If not, it will return an error.

#### Parameters

release

| | |
|---|---|
| unsignedDocument | the unsigned document |
| issuerProvidedData | the issuer provided data |