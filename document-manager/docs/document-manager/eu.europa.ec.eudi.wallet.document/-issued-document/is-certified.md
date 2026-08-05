//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[isCertified](is-certified.md)

# isCertified

[release]\
suspend fun [isCertified](is-certified.md)(): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the document is certified.

A document is certified if it has no pending credentials, meaning all credentials have been signed by an issuer.

#### Return

true if the document is certified, false otherwise

[release]\
open override val [~~isCertified~~](is-certified.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)

---

### Deprecated

Use method isCertified()

---