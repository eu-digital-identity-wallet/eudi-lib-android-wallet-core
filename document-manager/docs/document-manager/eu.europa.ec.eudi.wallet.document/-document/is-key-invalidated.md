//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[Document](index.md)/[isKeyInvalidated](is-key-invalidated.md)

# isKeyInvalidated

[release]\
abstract val [~~isKeyInvalidated~~](is-key-invalidated.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)

---

### Deprecated

For UnsignedDocument, use getPoPSigners() which filters out invalidated keys. For IssuedDocument, use findCredential()?.isInvalidated()

---