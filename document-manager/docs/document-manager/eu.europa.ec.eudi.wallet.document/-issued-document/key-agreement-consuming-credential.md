//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[keyAgreementConsumingCredential](key-agreement-consuming-credential.md)

# keyAgreementConsumingCredential

[release]\
suspend fun [keyAgreementConsumingCredential](key-agreement-consuming-credential.md)(otherPublicKey: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[SharedSecret](../-shared-secret/index.md)&gt;

Performs key agreement with a document credential and applies the credential policy.

This method finds a valid credential, uses it to establish a shared secret with the provided public key, and then applies the document's credential policy to the used credential. This implementation follows the document's credential policy: for OneTimeUse, the credential is deleted after use, and for RotateUse, the credential's usage count is incremented.

#### Return

A [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) containing the [SharedSecret](../-shared-secret/index.md) or an exception if the operation failed

#### Parameters

release

| | |
|---|---|
| otherPublicKey | The public key of the other party as a byte array |
| keyUnlockData | Optional data required to unlock the key if it's protected |