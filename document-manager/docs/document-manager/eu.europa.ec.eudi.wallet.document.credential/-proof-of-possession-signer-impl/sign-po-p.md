//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[ProofOfPossessionSignerImpl](index.md)/[signPoP](sign-po-p.md)

# signPoP

[release]\
open suspend override fun [signPoP](sign-po-p.md)(dataToSign: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData?): EcSignature

Creates a Proof of Possession signature by delegating to the secure area associated with the credential.

#### Return

An EcSignature containing the signature data.

#### Parameters

release

| | |
|---|---|
| dataToSign | The data bytes to be signed. |
| keyUnlockData | Optional data required to unlock the key for signing operations. |