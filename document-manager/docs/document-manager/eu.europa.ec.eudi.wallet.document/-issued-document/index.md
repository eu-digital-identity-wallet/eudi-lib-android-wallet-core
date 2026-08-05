//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)

# IssuedDocument

[release]\
class [IssuedDocument](index.md)(baseDocument: Document) : [Document](../-document/index.md)

Represents an Issued Document in the EUDI Wallet.

This class models a document that has been issued and stored in the wallet. It provides methods to access the document's data, verify its validity, and perform cryptographic operations using the document's credentials. Documents follow a specific credential policy that determines how credentials are used and managed after cryptographic operations.

The document's credentials are managed according to the specified [credentialPolicy](credential-policy.md), which can either rotate credentials after use or enforce one-time use.

## Constructors

| | |
|---|---|
| [IssuedDocument](-issued-document.md) | [release]<br>constructor(baseDocument: Document) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [createdAt](created-at.md) | [release]<br>open override val [createdAt](created-at.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)<br>The timestamp when the document was created in the wallet |
| [credentialPolicy](credential-policy.md) | [release]<br>val [credentialPolicy](credential-policy.md): [CreateDocumentSettings.CredentialPolicy](../-create-document-settings/-credential-policy/index.md)<br>The credential policy associated with this document. |
| [data](data.md) | [release]<br>val [data](data.md): [DocumentData](../../eu.europa.ec.eudi.wallet.document.format/-document-data/index.md)<br>The document data in its format-specific representation |
| [documentManagerId](document-manager-id.md) | [release]<br>open override val [documentManagerId](document-manager-id.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The identifier of the [DocumentManager](../-document-manager/index.md) that manages this document |
| [format](format.md) | [release]<br>open override val [format](format.md): [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md)<br>The format specification of the document (e.g., MsoMdoc, SdJwtVc) |
| [id](id.md) | [release]<br>open override val [id](id.md): [DocumentId](../-document-id/index.md)<br>The unique identifier of the document |
| [isCertified](is-certified.md) | [release]<br>open override val [~~isCertified~~](is-certified.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isKeyInvalidated](is-key-invalidated.md) | [release]<br>open override val [~~isKeyInvalidated~~](is-key-invalidated.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [issuedAt](issued-at.md) | [release]<br>val [issuedAt](issued-at.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)<br>The timestamp when the document was issued by the issuer |
| [issuerMetadata](issuer-metadata.md) | [release]<br>open override val [issuerMetadata](issuer-metadata.md): [IssuerMetadata](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/index.md)?<br>The document metadata provided by the issuer |
| [issuerProvidedData](issuer-provided-data.md) | [release]<br>val [~~issuerProvidedData~~](issuer-provided-data.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html) |
| [keyAlias](key-alias.md) | [release]<br>open override val [~~keyAlias~~](key-alias.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [keyInfo](key-info.md) | [release]<br>open override val [~~keyInfo~~](key-info.md): KeyInfo |
| [name](name.md) | [release]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The human-readable name of the document |
| [nameSpacedDataJSONObject](../name-spaced-data-j-s-o-n-object.md) | [release]<br>@get:[JvmName](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;nameSpacedDataAsJSONObject&quot;)<br>val [IssuedDocument](index.md).[nameSpacedDataJSONObject](../name-spaced-data-j-s-o-n-object.md): [JSONObject](https://developer.android.com/reference/kotlin/org/json/JSONObject.html)<br>Extension function to convert [IssuedDocument](index.md)'s nameSpacedData to [JSONObject](https://developer.android.com/reference/kotlin/org/json/JSONObject.html) Applicable only when [IssuedDocument.data](data.md) returns [MsoMdocData](../../eu.europa.ec.eudi.wallet.document.format/-mso-mdoc-data/index.md) |
| [publicKeyCoseBytes](public-key-cose-bytes.md) | [release]<br>open override val [~~publicKeyCoseBytes~~](public-key-cose-bytes.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html) |
| [secureArea](secure-area.md) | [release]<br>open override val [~~secureArea~~](secure-area.md): SecureArea |
| [validFrom](valid-from.md) | [release]<br>val [~~validFrom~~](valid-from.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html) |
| [validUntil](valid-until.md) | [release]<br>val [~~validUntil~~](valid-until.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html) |

## Functions

| Name | Summary |
|---|---|
| [consumingCredential](consuming-credential.md) | [release]<br>suspend fun &lt;[T](consuming-credential.md)&gt; [consumingCredential](consuming-credential.md)(credentialContext: suspend SecureAreaBoundCredential.() -&gt; [T](consuming-credential.md)): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[T](consuming-credential.md)&gt;<br>Performs an operation with a valid credential and handles usage policy enforcement. |
| [credentialsCount](credentials-count.md) | [release]<br>open suspend override fun [credentialsCount](credentials-count.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the number of credentials that pass structural validity checks. |
| [findCredential](find-credential.md) | [release]<br>suspend fun [findCredential](find-credential.md)(now: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)? = null): SecureAreaBoundCredential?<br>Finds the most appropriate credential for the current time or a specified time. |
| [getCredentials](get-credentials.md) | [release]<br>suspend fun [getCredentials](get-credentials.md)(): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureAreaBoundCredential&gt;<br>Retrieves all credentials associated with this document that pass structural validity checks. |
| [getValidFrom](get-valid-from.md) | [release]<br>suspend fun [getValidFrom](get-valid-from.md)(): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)&gt;<br>Retrieves the start date from which the document's credential is valid. |
| [getValidUntil](get-valid-until.md) | [release]<br>suspend fun [getValidUntil](get-valid-until.md)(): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)&gt;<br>Retrieves the end date until which the document's credential is valid. |
| [initialCredentialsCount](initial-credentials-count.md) | [release]<br>fun [initialCredentialsCount](initial-credentials-count.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>Retrieves the initial number of credentials for this document. The number of credentials initially created for this document. |
| [isCertified](is-certified.md) | [release]<br>suspend fun [isCertified](is-certified.md)(): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the document is certified. |
| [isValidAt](is-valid-at.md) | [release]<br>fun [~~isValidAt~~](is-valid-at.md)(time: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the document is valid at a specified point in time. |
| [keyAgreement](key-agreement.md) | [release]<br>fun [~~keyAgreement~~](key-agreement.md)(otherPublicKey: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Outcome](../-outcome/index.md)&lt;[SharedSecret](../-shared-secret/index.md)&gt;<br>Performs a key agreement protocol to create a shared secret with another party. |
| [keyAgreementConsumingCredential](key-agreement-consuming-credential.md) | [release]<br>suspend fun [keyAgreementConsumingCredential](key-agreement-consuming-credential.md)(otherPublicKey: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[SharedSecret](../-shared-secret/index.md)&gt;<br>Performs key agreement with a document credential and applies the credential policy. |
| [sign](sign.md) | [release]<br>fun [~~sign~~](sign.md)(dataToSign: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Outcome](../-outcome/index.md)&lt;EcSignature&gt;<br>Signs data using the document's cryptographic key. |
| [signConsumingCredential](sign-consuming-credential.md) | [release]<br>suspend fun [signConsumingCredential](sign-consuming-credential.md)(dataToSign: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;EcSignature&gt;<br>Signs data with a document credential and applies the credential policy. |