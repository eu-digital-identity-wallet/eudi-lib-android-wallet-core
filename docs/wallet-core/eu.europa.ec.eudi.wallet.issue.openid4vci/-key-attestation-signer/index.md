//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[KeyAttestationSigner](index.md)

# KeyAttestationSigner

[androidJvm]\
class [KeyAttestationSigner](index.md) : Signer&lt;KeyAttestationJWT&gt;

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [javaAlgorithm](java-algorithm.md) | [androidJvm]<br>open override val [javaAlgorithm](java-algorithm.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [keyLockedException](key-locked-exception.md) | [androidJvm]<br>var [keyLockedException](key-locked-exception.md): KeyLockedException? |
| [signer](signer.md) | [androidJvm]<br>val [signer](signer.md): ProofOfPossessionSigner |

## Functions

| Name | Summary |
|---|---|
| [acquire](acquire.md) | [androidJvm]<br>open suspend override fun [acquire](acquire.md)(): SignOperation&lt;KeyAttestationJWT&gt; |
| [release](release.md) | [androidJvm]<br>open suspend override fun [release](release.md)(signOperation: SignOperation&lt;KeyAttestationJWT&gt;?) |
