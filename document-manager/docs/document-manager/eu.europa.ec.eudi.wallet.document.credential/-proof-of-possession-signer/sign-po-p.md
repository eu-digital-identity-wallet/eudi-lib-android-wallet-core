//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[ProofOfPossessionSigner](index.md)/[signPoP](sign-po-p.md)

# signPoP

[release]\
abstract suspend fun [signPoP](sign-po-p.md)(dataToSign: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData?): EcSignature

Signs the provided data to create a Proof of Possession signature.

#### Return

An EcSignature containing the signature data.

#### Parameters

release

| | |
|---|---|
| dataToSign | The data bytes to be signed. |
| keyUnlockData | Optional data required to unlock the key for signing operations. |