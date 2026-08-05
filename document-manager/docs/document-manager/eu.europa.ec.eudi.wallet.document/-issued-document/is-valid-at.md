//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[isValidAt](is-valid-at.md)

# isValidAt

[release]\
fun [~~isValidAt~~](is-valid-at.md)(time: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)

---

### Deprecated

Use findCredential() instead to check validity at a specific time. If findCredential() returns null, the document is not valid.

---

Checks if the document is valid at a specified point in time.

A document is considered valid if it has at least one valid credential at the specified time according to the findCredential criteria.

#### Return

true if the document is valid at the specified time, false otherwise

#### Parameters

release

| | |
|---|---|
| time | The timestamp at which to check validity |