//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[Offer](../index.md)/[TxCodeSpec](index.md)

# TxCodeSpec

[androidJvm]\
data class [TxCodeSpec](index.md)(val inputMode: [Offer.TxCodeSpec.InputMode](-input-mode/index.md) = NUMERIC, val length: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?, val description: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null)

Specification for a transaction code.

## Constructors

| | |
|---|---|
| [TxCodeSpec](-tx-code-spec.md) | [androidJvm]<br>constructor(inputMode: [Offer.TxCodeSpec.InputMode](-input-mode/index.md) = NUMERIC, length: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?, description: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |

## Types

| Name | Summary |
|---|---|
| [InputMode](-input-mode/index.md) | [androidJvm]<br>enum [InputMode](-input-mode/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[Offer.TxCodeSpec.InputMode](-input-mode/index.md)&gt; <br>The input mode for the transaction code. |

## Properties

| Name | Summary |
|---|---|
| [description](description.md) | [androidJvm]<br>val [description](description.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>a description of the transaction code |
| [inputMode](input-mode.md) | [androidJvm]<br>val [inputMode](input-mode.md): [Offer.TxCodeSpec.InputMode](-input-mode/index.md)<br>the input mode for the transaction code |
| [length](length.md) | [androidJvm]<br>val [length](length.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?<br>the length of the transaction code |
