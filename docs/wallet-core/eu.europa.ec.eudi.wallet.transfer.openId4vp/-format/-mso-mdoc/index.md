//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[Format](../index.md)/[MsoMdoc](index.md)

# MsoMdoc

[release]\
data class [MsoMdoc](index.md)(val issuerAuthAlgorithms: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, val deviceAuthAlgorithms: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) : [Format](../index.md)

Mobile Security Object document format (ISO 18013-5 mDL).

This format represents ISO 18013-5 mobile driving license documents and similar mobile security objects. It provides a standardized way to present identity credentials in mobile environments with strong cryptographic security.

## Constructors

| | |
|---|---|
| [MsoMdoc](-mso-mdoc.md) | [release]<br>constructor(issuerAuthAlgorithms: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, deviceAuthAlgorithms: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [deviceAuthAlgorithms](device-auth-algorithms.md) | [release]<br>val [deviceAuthAlgorithms](device-auth-algorithms.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt; |
| [issuerAuthAlgorithms](issuer-auth-algorithms.md) | [release]<br>val [issuerAuthAlgorithms](issuer-auth-algorithms.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt; |