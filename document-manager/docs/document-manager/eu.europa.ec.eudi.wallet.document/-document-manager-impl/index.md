//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManagerImpl](index.md)

# DocumentManagerImpl

class [DocumentManagerImpl](index.md)(val identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val storage: Storage, val secureAreaRepository: SecureAreaRepository, val ktorHttpClientFactory: () -&gt; HttpClient? = null) : [DocumentManager](../-document-manager/index.md)

Default implementation of the [DocumentManager](../-document-manager/index.md) interface for the EUDI Wallet.

This implementation provides the core functionality for managing digital documents in the EUDI Wallet ecosystem, including:

- 
   Creation of documents with multiple supported formats (MSO mDoc, SD-JWT VC)
- 
   Secure storage and retrieval of documents using provided storage mechanisms
- 
   Management of document lifecycle and state transitions
- 
   Integration with secure area for cryptographic operations and key management

The implementation maintains strict document identity boundaries by using a unique document manager identifier to ensure that documents managed by one instance cannot be accessed or modified by another instance.

#### Parameters

release

| | |
|---|---|
| identifier | Unique identifier for this document manager instance |
| storage | Storage implementation for persisting document data |
| secureAreaRepository | Repository for secure key management and cryptographic operations |
| ktorHttpClientFactory | Optional factory method to create HTTP clients |

## Constructors

| | |
|---|---|
| [DocumentManagerImpl](-document-manager-impl.md) | [release]<br>constructor(identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), storage: Storage, secureAreaRepository: SecureAreaRepository, ktorHttpClientFactory: () -&gt; HttpClient? = null)<br>Creates a new DocumentManagerImpl with the required dependencies |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [identifier](identifier.md) | [release]<br>open override val [identifier](identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>Unique identifier for this document manager instance, used to scope document access |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [release]<br>val [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient?<br>Optional factory to provide custom HTTP clients for network operations |
| [secureAreaRepository](secure-area-repository.md) | [release]<br>open override val [secureAreaRepository](secure-area-repository.md): SecureAreaRepository<br>Repository for cryptographic operations and secure key management |
| [storage](storage.md) | [release]<br>open override val [storage](storage.md): Storage<br>Persistent storage implementation for document data |

## Functions

| Name | Summary |
|---|---|
| [createDocument](create-document.md) | [release]<br>open override fun [createDocument](create-document.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), createSettings: [CreateDocumentSettings](../-create-document-settings/index.md), issuerMetadata: [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md)? = null): [Outcome](../-outcome/index.md)&lt;[UnsignedDocument](../-unsigned-document/index.md)&gt;<br>Create a new document. This method will create a new document with the given format and keys settings. If the document is successfully created, it will return an [UnsignedDocument](../-unsigned-document/index.md). This [UnsignedDocument](../-unsigned-document/index.md) contains the keys and the method to proof the ownership of the keys, that can be used with an issuer to retrieve the document's claims. After that the document can be stored using [storeIssuedDocument](store-issued-document.md) or [storeDeferredDocument](store-deferred-document.md). |
| [deleteDocumentById](delete-document-by-id.md) | [release]<br>open override fun [deleteDocumentById](delete-document-by-id.md)(documentId: [DocumentId](../-document-id/index.md)): [Outcome](../-outcome/index.md)&lt;[ProofOfDeletion](../-proof-of-deletion/index.md)?&gt;<br>Delete a document by its identifier. |
| [getDocumentById](get-document-by-id.md) | [release]<br>open override fun [getDocumentById](get-document-by-id.md)(documentId: [DocumentId](../-document-id/index.md)): [Document](../-document/index.md)?<br>Retrieve a document by its identifier. |
| [getDocuments](get-documents.md) | [release]<br>open override fun [getDocuments](get-documents.md)(predicate: ([Document](../-document/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)? = null): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Document](../-document/index.md)&gt;<br>Retrieve all documents. |
| [getDocuments](../get-documents.md) | [release]<br>inline fun &lt;[T](../get-documents.md) : [Document](../-document/index.md)&gt; [DocumentManager](../-document-manager/index.md).[getDocuments](../get-documents.md)(): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[T](../get-documents.md)&gt;<br>DocumentManager Extension function that returns a list of documents of type [T](../get-documents.md). If [T](../get-documents.md) is [IssuedDocument](../-issued-document/index.md), then only [IssuedDocument](../-issued-document/index.md) will be returned. If [T](../get-documents.md) is [UnsignedDocument](../-unsigned-document/index.md), then only [UnsignedDocument](../-unsigned-document/index.md) will be returned, excluding [DeferredDocument](../-deferred-document/index.md). If [T](../get-documents.md) is [DeferredDocument](../-deferred-document/index.md), then only [DeferredDocument](../-deferred-document/index.md) will be returned. |
| [storeDeferredDocument](store-deferred-document.md) | [release]<br>open override fun [storeDeferredDocument](store-deferred-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), relatedData: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)): [Outcome](../-outcome/index.md)&lt;[DeferredDocument](../-deferred-document/index.md)&gt;<br>Store an unsigned document for deferred issuance. This method will store the document with the related to the issuance data. |
| [storeIssuedDocument](store-issued-document.md) | [release]<br>open override fun [storeIssuedDocument](store-issued-document.md)(unsignedDocument: [UnsignedDocument](../-unsigned-document/index.md), issuerProvidedData: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerProvidedCredential](../../eu.europa.ec.eudi.wallet.document.credential/-issuer-provided-credential/index.md)&gt;): [Outcome](../-outcome/index.md)&lt;[IssuedDocument](../-issued-document/index.md)&gt;<br>Store an issued document. This method will store the document with its issuer provided data. |