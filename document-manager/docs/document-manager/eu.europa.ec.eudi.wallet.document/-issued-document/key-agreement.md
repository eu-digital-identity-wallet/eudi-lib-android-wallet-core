//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[keyAgreement](key-agreement.md)

# keyAgreement

[release]\
fun [~~keyAgreement~~](key-agreement.md)(otherPublicKey: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Outcome](../-outcome/index.md)&lt;[SharedSecret](../-shared-secret/index.md)&gt;

---

### Deprecated

use keyAgreementConsumingCredential method instead

---

Performs a key agreement protocol to create a shared secret with another party.

This method uses the document's private key and the other party's public key to establish a shared secret through a cryptographic key agreement protocol. After the operation, it applies the document's credential policy.

#### Return

An [Outcome](../-outcome/index.md) containing either the [SharedSecret](../-shared-secret/index.md) or a failure reason

#### Parameters

release

| | |
|---|---|
| otherPublicKey | The public key of the other party as a byte array |
| keyUnlockData | Optional data required to unlock the document's key if it's protected |