//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[Document](index.md)/[publicKeyCoseBytes](public-key-cose-bytes.md)

# publicKeyCoseBytes

[release]\
abstract val [~~publicKeyCoseBytes~~](public-key-cose-bytes.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)

---

### Deprecated

For UnsignedDocument, use getPoPSigners().first().getKeyInfo().publicKey.toCoseBytes. For IssuedDocument, use findCredential()?.secureArea.getKeyInfo().publicKey.toCoseBytes

---