//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[credentialsCount](credentials-count.md)

# credentialsCount

[release]\
open suspend override fun [credentialsCount](credentials-count.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)

Returns the number of credentials that pass structural validity checks.

Delegates to [getCredentials](get-credentials.md), which does **not** filter by temporal validity. This count may include expired or not-yet-valid credentials. To check how many credentials are currently usable, filter [getCredentials](get-credentials.md) by `validFrom`/`validUntil` or use [findCredential](find-credential.md) to check if at least one is valid.