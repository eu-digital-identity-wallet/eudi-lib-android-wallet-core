//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[BatchProofSigner](index.md)

# BatchProofSigner

[release]\
class [BatchProofSigner](index.md)(val signers: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ProofOfPossessionSigner&gt;, keyUnlockData: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), KeyUnlockData?&gt;? = null) : BatchSigner&lt;JwtBindingKey&gt;

## Constructors

| | |
|---|---|
| [BatchProofSigner](-batch-proof-signer.md) | [release]<br>constructor(signers: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ProofOfPossessionSigner&gt;, keyUnlockData: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), KeyUnlockData?&gt;? = null) |

## Properties

| Name | Summary |
|---|---|
| [algorithm](algorithm.md) | [release]<br>val [algorithm](algorithm.md): Algorithm |
| [javaAlgorithm](java-algorithm.md) | [release]<br>open override val [javaAlgorithm](java-algorithm.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [keyLockedException](key-locked-exception.md) | [release]<br>var [keyLockedException](key-locked-exception.md): KeyLockedException? |
| [signers](signers.md) | [release]<br>val [signers](signers.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ProofOfPossessionSigner&gt; |

## Functions

| Name | Summary |
|---|---|
| [authenticate](authenticate.md) | [release]<br>open suspend override fun [authenticate](authenticate.md)(): BatchSignOperation&lt;JwtBindingKey&gt; |
| [release](release.md) | [release]<br>open suspend override fun [release](release.md)(signOps: BatchSignOperation&lt;JwtBindingKey&gt;?) |