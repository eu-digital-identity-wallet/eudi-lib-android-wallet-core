//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[ProofOfPossessionSigner](index.md)

# ProofOfPossessionSigner

interface [ProofOfPossessionSigner](index.md)

Interface for creating Proof of Possession (PoP) signatures.

Implementations of this interface are responsible for generating cryptographic signatures that prove possession of a private key.

#### Inheritors

| |
|---|
| [ProofOfPossessionSignerImpl](../-proof-of-possession-signer-impl/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [keyAlias](key-alias.md) | [release]<br>abstract val [keyAlias](key-alias.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The alias of the key used for signing. |
| [secureArea](secure-area.md) | [release]<br>abstract val [secureArea](secure-area.md): SecureArea<br>The secure area associated with the key. |

## Functions

| Name | Summary |
|---|---|
| [getKeyInfo](get-key-info.md) | [release]<br>abstract suspend fun [getKeyInfo](get-key-info.md)(): KeyInfo<br>The alias of the key used for signing. |
| [signPoP](sign-po-p.md) | [release]<br>abstract suspend fun [signPoP](sign-po-p.md)(dataToSign: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), keyUnlockData: KeyUnlockData?): EcSignature<br>Signs the provided data to create a Proof of Possession signature. |