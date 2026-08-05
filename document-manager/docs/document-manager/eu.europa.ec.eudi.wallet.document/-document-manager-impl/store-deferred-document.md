//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManagerImpl](index.md)/[storeDeferredDocument](store-deferred-document.md)

# storeDeferredDocument

[release]\
open override fun [storeDeferredDocument](store-deferred-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), relatedData: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)): [Outcome](../-outcome/index.md)&lt;[DeferredDocument](../-deferred-document/index.md)&gt;

Store an unsigned document for deferred issuance. This method will store the document with the related to the issuance data.

#### Return

the result of the storage. If successful, it will return the [DeferredDocument](../-deferred-document/index.md). If not, it will return an error.

#### Parameters

release

| | |
|---|---|
| unsignedDocument | the unsigned document |
| relatedData | the related data |