//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[Document](index.md)/[credentialsCount](credentials-count.md)

# credentialsCount

[release]\
abstract suspend fun [credentialsCount](credentials-count.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)

Returns the number of credentials associated with this document that pass structural validity checks.

For [UnsignedDocument](../-unsigned-document/index.md), this counts credentials that can be used for proof of possession. For [IssuedDocument](../-issued-document/index.md), this counts credentials according to the credential policy but does **not** filter by temporal validity (`validFrom`/`validUntil`). The count may include expired or not-yet-valid credentials. Use [IssuedDocument.findCredential](../-issued-document/find-credential.md) to check if a credential is valid at a specific point in time.

#### Return

The number of credentials that pass structural validity checks