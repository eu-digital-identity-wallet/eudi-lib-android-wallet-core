//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[signConsumingCredential](sign-consuming-credential.md)

# signConsumingCredential

[release]\
suspend fun [signConsumingCredential](sign-consuming-credential.md)(dataToSign: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData? = null): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;EcSignature&gt;

Signs data with a document credential and applies the credential policy.

This method finds a valid credential, uses it to sign the provided data, and then applies the document's credential policy to the used credential.

#### Return

A [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) containing the EcSignature or an exception if the operation failed

#### Parameters

release

| | |
|---|---|
| dataToSign | The byte array containing the data to be signed |
| keyUnlockData | Optional data required to unlock the key if it's protected |