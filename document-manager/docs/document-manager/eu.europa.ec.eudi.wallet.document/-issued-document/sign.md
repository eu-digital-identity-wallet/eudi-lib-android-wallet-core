//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[sign](sign.md)

# sign

[release]\
fun [~~sign~~](sign.md)(dataToSign: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Outcome](../-outcome/index.md)&lt;EcSignature&gt;

---

### Deprecated

use signConsumingCredential method instead

---

Signs data using the document's cryptographic key.

This method uses the document's credential to create a cryptographic signature for the provided data. After signing, it applies the document's credential policy.

#### Return

An [Outcome](../-outcome/index.md) containing either the EcSignature or a failure reason

#### Parameters

release

| | |
|---|---|
| dataToSign | The byte array containing the data to be signed |
| keyUnlockData | Optional data required to unlock the key if it's protected |