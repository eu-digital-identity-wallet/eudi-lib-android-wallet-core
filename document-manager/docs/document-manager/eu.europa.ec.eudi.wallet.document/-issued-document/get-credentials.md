//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[getCredentials](get-credentials.md)

# getCredentials

[release]\
suspend fun [getCredentials](get-credentials.md)(): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureAreaBoundCredential&gt;

Retrieves all credentials associated with this document that pass structural validity checks.

This method filters the document's credentials based on several criteria:

- 
   Only certified credentials bound to a secure area
- 
   Only credentials that are not invalidated
- 
   Only credentials that belong to the current document manager
- 
   For OneTimeUse policy, only credentials that haven't been used (usageCount == 0)
- 
   For RotateUse policy, all credentials regardless of usage count

**Note:** This method does **not** filter by temporal validity (`validFrom`/`validUntil`). The returned list may include credentials that are expired or not yet valid. Use [findCredential](find-credential.md) to obtain a credential that is valid at a specific point in time.

#### Return

A list of SecureAreaBoundCredential objects that pass structural validity checks