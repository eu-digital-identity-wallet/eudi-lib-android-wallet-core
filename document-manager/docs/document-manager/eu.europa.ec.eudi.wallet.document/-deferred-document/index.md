//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DeferredDocument](index.md)

# DeferredDocument

[release]\
class [DeferredDocument](index.md)(baseDocument: Document, val relatedData: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)) : [UnsignedDocument](../-unsigned-document/index.md)

Represents a Deferred Document in the EUDI Wallet.

A Deferred Document extends the [UnsignedDocument](../-unsigned-document/index.md) class and represents a document that is waiting to be issued. It contains additional related data necessary for the issuance process. Deferred documents are created when a document issuance request has been initiated but the actual issuance is pending or will happen at a later time.

## Constructors

| | |
|---|---|
| [DeferredDocument](-deferred-document.md) | [release]<br>constructor(baseDocument: Document, relatedData: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [createdAt](../-unsigned-document/created-at.md) | [release]<br>open override val [createdAt](../-unsigned-document/created-at.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)<br>The timestamp when the document was created in the wallet |
| [documentManagerId](../-unsigned-document/document-manager-id.md) | [release]<br>open override val [documentManagerId](../-unsigned-document/document-manager-id.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The identifier of the DocumentManager that manages this document |
| [format](../-unsigned-document/format.md) | [release]<br>open override val [format](../-unsigned-document/format.md): [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md)<br>The format specification of the document (e.g., MsoMdoc, SdJwtVc) |
| [id](../-unsigned-document/id.md) | [release]<br>open override val [id](../-unsigned-document/id.md): [DocumentId](../-document-id/index.md)<br>The unique identifier of the document |
| [isCertified](../-unsigned-document/is-certified.md) | [release]<br>open override val [~~isCertified~~](../-unsigned-document/is-certified.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = false |
| [isKeyInvalidated](../-unsigned-document/is-key-invalidated.md) | [release]<br>open override val [~~isKeyInvalidated~~](../-unsigned-document/is-key-invalidated.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [issuerMetadata](../-unsigned-document/issuer-metadata.md) | [release]<br>open override val [issuerMetadata](../-unsigned-document/issuer-metadata.md): [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md)?<br>The document metadata provided by the issuer, if available |
| [keyAlias](../-unsigned-document/key-alias.md) | [release]<br>open override val [~~keyAlias~~](../-unsigned-document/key-alias.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [keyInfo](../-unsigned-document/key-info.md) | [release]<br>open override val [~~keyInfo~~](../-unsigned-document/key-info.md): KeyInfo |
| [name](../-unsigned-document/name.md) | [release]<br>open override var [name](../-unsigned-document/name.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The human-readable name of the document |
| [publicKeyCoseBytes](../-unsigned-document/public-key-cose-bytes.md) | [release]<br>open override val [~~publicKeyCoseBytes~~](../-unsigned-document/public-key-cose-bytes.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html) |
| [relatedData](related-data.md) | [release]<br>val [relatedData](related-data.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)<br>Additional data associated with this document that is needed for                        the deferred issuance process (e.g., issuance request identifiers or tokens) |
| [secureArea](../-unsigned-document/secure-area.md) | [release]<br>open override val [~~secureArea~~](../-unsigned-document/secure-area.md): SecureArea |

## Functions

| Name | Summary |
|---|---|
| [credentialsCount](../-unsigned-document/credentials-count.md) | [release]<br>open suspend override fun [credentialsCount](../-unsigned-document/credentials-count.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the number of valid credentials associated with this document. |
| [getPoPSigners](../-unsigned-document/get-po-p-signers.md) | [release]<br>suspend fun [getPoPSigners](../-unsigned-document/get-po-p-signers.md)(): [ProofOfPossessionSigners](../../eu.europa.ec.eudi.wallet.document.credential/-proof-of-possession-signers/index.md)<br>Creates proof of possession signers for the document credentials. |