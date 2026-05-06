//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[KeyAttestationSigner](index.md)

# KeyAttestationSigner

[release]\
class [KeyAttestationSigner](index.md) : Signer&lt;KeyAttestationJWT&gt;

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [javaAlgorithm](java-algorithm.md) | [release]<br>open override val [javaAlgorithm](java-algorithm.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [keyLockedException](key-locked-exception.md) | [release]<br>var [keyLockedException](key-locked-exception.md): KeyLockedException? |
| [signer](signer.md) | [release]<br>val [signer](signer.md): ProofOfPossessionSigner |

## Functions

| Name | Summary |
|---|---|
| [acquire](acquire.md) | [release]<br>open suspend override fun [acquire](acquire.md)(): SignOperation&lt;KeyAttestationJWT&gt; |
| [release](release.md) | [release]<br>open suspend override fun [release](release.md)(signOperation: SignOperation&lt;KeyAttestationJWT&gt;?) |