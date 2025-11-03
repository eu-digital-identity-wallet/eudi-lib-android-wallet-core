//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DPoPSigner](index.md)

# DPoPSigner

[androidJvm]\
class [DPoPSigner](index.md) : Signer&lt;JWK&gt;

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [algorithm](algorithm.md) | [androidJvm]<br>val [algorithm](algorithm.md): Algorithm |
| [javaAlgorithm](java-algorithm.md) | [androidJvm]<br>open override val [javaAlgorithm](java-algorithm.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |

## Functions

| Name | Summary |
|---|---|
| [acquire](acquire.md) | [androidJvm]<br>open suspend override fun [acquire](acquire.md)(): SignOperation&lt;JWK&gt; |
| [release](release.md) | [androidJvm]<br>open suspend override fun [release](release.md)(signOperation: SignOperation&lt;JWK&gt;?) |
