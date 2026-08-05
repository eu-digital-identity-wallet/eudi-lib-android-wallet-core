//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[UnsignedDocument](index.md)

# UnsignedDocument

open class [UnsignedDocument](index.md)(baseDocument: Document) : [Document](../-document/index.md)

Represents a document that has been created but not yet fully issued.

An UnsignedDocument contains one or more credentials that have not been certified by an issuer. It provides access to these credentials through proof-of-possession signers, which can be used during issuance procedures to prove possession of private keys and receive the issuer's certification.

#### Inheritors

| |
|---|
| [DeferredDocument](../-deferred-document/index.md) |

## Constructors

| | |
|---|---|
| [UnsignedDocument](-unsigned-document.md) | [release]<br>constructor(baseDocument: Document) |

## Properties

| Name | Summary |
|---|---|
| [createdAt](created-at.md) | [release]<br>open override val [createdAt](created-at.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)<br>The timestamp when the document was created in the wallet |
| [documentManagerId](document-manager-id.md) | [release]<br>open override val [documentManagerId](document-manager-id.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The identifier of the DocumentManager that manages this document |
| [format](format.md) | [release]<br>open override val [format](format.md): [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md)<br>The format specification of the document (e.g., MsoMdoc, SdJwtVc) |
| [id](id.md) | [release]<br>open override val [id](id.md): [DocumentId](../-document-id/index.md)<br>The unique identifier of the document |
| [isCertified](is-certified.md) | [release]<br>open override val [~~isCertified~~](is-certified.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = false |
| [isKeyInvalidated](is-key-invalidated.md) | [release]<br>open override val [~~isKeyInvalidated~~](is-key-invalidated.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [issuerMetadata](issuer-metadata.md) | [release]<br>open override val [issuerMetadata](issuer-metadata.md): [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md)?<br>The document metadata provided by the issuer, if available |
| [keyAlias](key-alias.md) | [release]<br>open override val [~~keyAlias~~](key-alias.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [keyInfo](key-info.md) | [release]<br>open override val [~~keyInfo~~](key-info.md): KeyInfo |
| [name](name.md) | [release]<br>open override var [name](name.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The human-readable name of the document |
| [publicKeyCoseBytes](public-key-cose-bytes.md) | [release]<br>open override val [~~publicKeyCoseBytes~~](public-key-cose-bytes.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html) |
| [secureArea](secure-area.md) | [release]<br>open override val [~~secureArea~~](secure-area.md): SecureArea |

## Functions

| Name | Summary |
|---|---|
| [credentialsCount](credentials-count.md) | [release]<br>open suspend override fun [credentialsCount](credentials-count.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the number of valid credentials associated with this document. |
| [getPoPSigners](get-po-p-signers.md) | [release]<br>suspend fun [getPoPSigners](get-po-p-signers.md)(): [ProofOfPossessionSigners](../../eu.europa.ec.eudi.wallet.document.credential/-proof-of-possession-signers/index.md)<br>Creates proof of possession signers for the document credentials. |