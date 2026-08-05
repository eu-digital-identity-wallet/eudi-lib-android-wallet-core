//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManager](index.md)/[storeDeferredDocument](store-deferred-document.md)

# storeDeferredDocument

[release]\
abstract fun [storeDeferredDocument](store-deferred-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), relatedData: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)): [Outcome](../-outcome/index.md)&lt;[DeferredDocument](../-deferred-document/index.md)&gt;

Stores an unsigned document for deferred issuance processing.

This method is used when the document issuance process cannot be completed immediately and requires additional steps or time to complete. It stores the unsigned document  along with additional data needed to resume and complete the issuance process later.

Deferred issuance is useful in scenarios where:

- 
   The issuer requires multi-step verification
- 
   The issuance process has a time delay
- 
   Additional user actions are needed before issuance can complete

#### Return

An [Outcome](../-outcome/index.md) containing either:          - A success result with the stored [DeferredDocument](../-deferred-document/index.md)          - A failure result with an exception describing what went wrong

#### Parameters

release

| | |
|---|---|
| unsignedDocument | The unsigned document that is undergoing deferred issuance |
| relatedData | Binary data containing information needed to resume the issuance process |