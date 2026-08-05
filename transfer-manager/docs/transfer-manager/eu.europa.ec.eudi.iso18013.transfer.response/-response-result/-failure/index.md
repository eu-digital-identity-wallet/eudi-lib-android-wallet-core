//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../../index.md)/[ResponseResult](../index.md)/[Failure](index.md)

# Failure

[release]\
data class [Failure](index.md)(val throwable: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) : [ResponseResult](../index.md)

The response generation failed

## Constructors

| | |
|---|---|
| [Failure](-failure.md) | [release]<br>constructor(throwable: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [throwable](throwable.md) | [release]<br>val [throwable](throwable.md): [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)<br>the throwable |

## Functions

| Name | Summary |
|---|---|
| [getOrNull](../get-or-null.md) | [release]<br>open fun [getOrNull](../get-or-null.md)(): [Response](../../-response/index.md)?<br>Returns the response or null |
| [getOrThrow](../get-or-throw.md) | [release]<br>open fun [getOrThrow](../get-or-throw.md)(): [Response](../../-response/index.md)<br>Returns the response or throws the throwable |
| [toKotlinResult](../../../eu.europa.ec.eudi.iso18013.transfer/to-kotlin-result.md) | [release]<br>fun [ResponseResult](../index.md).[toKotlinResult](../../../eu.europa.ec.eudi.iso18013.transfer/to-kotlin-result.md)(): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[ResponseResult.Success](../-success/index.md)&gt;<br>Converts a [ResponseResult](../index.md) to a [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) of [ResponseResult.Success](../-success/index.md) |