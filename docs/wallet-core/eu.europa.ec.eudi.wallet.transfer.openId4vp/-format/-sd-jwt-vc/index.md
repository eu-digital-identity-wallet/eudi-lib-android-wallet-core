//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[Format](../index.md)/[SdJwtVc](index.md)

# SdJwtVc

data class [SdJwtVc](index.md)(val sdJwtAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, val kbJwtAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) : [Format](../index.md)

Selective Disclosure JWT Verifiable Credential format configuration.

This format supports SD-JWT (Selective Disclosure JSON Web Token) verifiable credentials, which allow holders to selectively disclose specific claims from their credentials while maintaining cryptographic integrity. The format requires configuration of algorithms for both the SD-JWT itself and optional Key Binding JWTs.

#### See also

| |
|---|
| Algorithm |

## Constructors

| | |
|---|---|
| [SdJwtVc](-sd-jwt-vc.md) | [androidJvm]<br>constructor(sdJwtAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, kbJwtAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [kbJwtAlgorithms](kb-jwt-algorithms.md) | [androidJvm]<br>val [kbJwtAlgorithms](kb-jwt-algorithms.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;<br>List of algorithms supported for Key Binding JWT operations |
| [sdJwtAlgorithms](sd-jwt-algorithms.md) | [androidJvm]<br>val [sdJwtAlgorithms](sd-jwt-algorithms.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;<br>List of algorithms supported for SD-JWT signature and verification |
