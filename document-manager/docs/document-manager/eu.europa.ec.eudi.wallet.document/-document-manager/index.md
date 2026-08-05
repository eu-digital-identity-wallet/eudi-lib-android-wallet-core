//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManager](index.md)

# DocumentManager

interface [DocumentManager](index.md)

The DocumentManager interface is the main entry point to interact with documents in the EUDI Wallet. It is a high-level abstraction that provides a simplified API to manage digital documents like credentials, certificates, and other identity documents.

The DocumentManager is responsible for:

- 
   Creating new documents with specific formats and security configurations
- 
   Managing the document lifecycle (creation, issuance, storage, retrieval, deletion)
- 
   Providing secure storage and access to documents
- 
   Supporting different document formats and credential types

Document creation follows a specific flow:

1. 
   Create an unsigned document with [createDocument](create-document.md)
2. 
   Use the resulting [UnsignedDocument](../-unsigned-document/index.md) with an issuer to obtain certified claims
3. 
   Store the document either as:
4. - 
      An [IssuedDocument](../-issued-document/index.md) with [storeIssuedDocument](store-issued-document.md) when claims are immediately available
   - 
      A [DeferredDocument](../-deferred-document/index.md) with [storeDeferredDocument](store-deferred-document.md) when issuance will complete later

To create a DocumentManager instance, use the companion object or the [Builder](-builder/index.md) class.

#### See also

| |
|---|
| [Builder](-builder/index.md) |
| [DocumentManagerImpl](../-document-manager-impl/index.md) | for the default implementation |

#### Inheritors

| |
|---|
| [DocumentManagerImpl](../-document-manager-impl/index.md) |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [release]<br>class [Builder](-builder/index.md)<br>Builder class to create a [DocumentManager](index.md) instance. |
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md)<br>Companion object to create a [DocumentManager](index.md) instance. |

## Properties

| Name | Summary |
|---|---|
| [identifier](identifier.md) | [release]<br>abstract val [identifier](identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>Unique identifier for this document manager instance |
| [secureAreaRepository](secure-area-repository.md) | [release]<br>abstract val [secureAreaRepository](secure-area-repository.md): SecureAreaRepository<br>Repository for secure key management and cryptographic operations |
| [storage](storage.md) | [release]<br>abstract val [storage](storage.md): Storage<br>Storage mechanism for persistent document data |

## Functions

| Name | Summary |
|---|---|
| [createDocument](create-document.md) | [release]<br>abstract fun [createDocument](create-document.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), createSettings: [CreateDocumentSettings](../-create-document-settings/index.md), issuerMetadata: [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md)? = null): [Outcome](../-outcome/index.md)&lt;[UnsignedDocument](../-unsigned-document/index.md)&gt;<br>Creates a new document with the specified format and security settings. |
| [deleteDocumentById](delete-document-by-id.md) | [release]<br>abstract fun [deleteDocumentById](delete-document-by-id.md)(documentId: [DocumentId](../-document-id/index.md)): [Outcome](../-outcome/index.md)&lt;[ProofOfDeletion](../-proof-of-deletion/index.md)?&gt;<br>Deletes a document by its unique identifier. |
| [getDocumentById](get-document-by-id.md) | [release]<br>abstract fun [getDocumentById](get-document-by-id.md)(documentId: [DocumentId](../-document-id/index.md)): [Document](../-document/index.md)?<br>Retrieves a document by its unique identifier. |
| [getDocuments](get-documents.md) | [release]<br>abstract fun [getDocuments](get-documents.md)(predicate: ([Document](../-document/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)? = null): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Document](../-document/index.md)&gt;<br>Retrieves all documents managed by this DocumentManager instance. |
| [getDocuments](../get-documents.md) | [release]<br>inline fun &lt;[T](../get-documents.md) : [Document](../-document/index.md)&gt; [DocumentManager](index.md).[getDocuments](../get-documents.md)(): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[T](../get-documents.md)&gt;<br>DocumentManager Extension function that returns a list of documents of type [T](../get-documents.md). If [T](../get-documents.md) is [IssuedDocument](../-issued-document/index.md), then only [IssuedDocument](../-issued-document/index.md) will be returned. If [T](../get-documents.md) is [UnsignedDocument](../-unsigned-document/index.md), then only [UnsignedDocument](../-unsigned-document/index.md) will be returned, excluding [DeferredDocument](../-deferred-document/index.md). If [T](../get-documents.md) is [DeferredDocument](../-deferred-document/index.md), then only [DeferredDocument](../-deferred-document/index.md) will be returned. |
| [storeDeferredDocument](store-deferred-document.md) | [release]<br>abstract fun [storeDeferredDocument](store-deferred-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), relatedData: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)): [Outcome](../-outcome/index.md)&lt;[DeferredDocument](../-deferred-document/index.md)&gt;<br>Stores an unsigned document for deferred issuance processing. |
| [storeIssuedDocument](store-issued-document.md) | [release]<br>abstract fun [storeIssuedDocument](store-issued-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), issuerProvidedData: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerProvidedCredential](../../eu.europa.ec.eudi.wallet.document.credential/-issuer-provided-credential/index.md)&gt;): [Outcome](../-outcome/index.md)&lt;[IssuedDocument](../-issued-document/index.md)&gt;<br>Stores a document that has completed the issuance process with an issuer. |