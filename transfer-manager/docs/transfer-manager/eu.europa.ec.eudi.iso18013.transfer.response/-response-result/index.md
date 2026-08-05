//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[ResponseResult](index.md)

# ResponseResult

sealed interface [ResponseResult](index.md)

Represents the result of a response

#### Inheritors

| |
|---|
| [Success](-success/index.md) |
| [Failure](-failure/index.md) |

## Types

| Name | Summary |
|---|---|
| [Failure](-failure/index.md) | [release]<br>data class [Failure](-failure/index.md)(val throwable: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) : [ResponseResult](index.md)<br>The response generation failed |
| [Success](-success/index.md) | [release]<br>data class [Success](-success/index.md)(val response: [Response](../-response/index.md)) : [ResponseResult](index.md)<br>The response generation was successful |

## Functions

| Name | Summary |
|---|---|
| [getOrNull](get-or-null.md) | [release]<br>open fun [getOrNull](get-or-null.md)(): [Response](../-response/index.md)?<br>Returns the response or null |
| [getOrThrow](get-or-throw.md) | [release]<br>open fun [getOrThrow](get-or-throw.md)(): [Response](../-response/index.md)<br>Returns the response or throws the throwable |
| [toKotlinResult](../../eu.europa.ec.eudi.iso18013.transfer/to-kotlin-result.md) | [release]<br>fun [ResponseResult](index.md).[toKotlinResult](../../eu.europa.ec.eudi.iso18013.transfer/to-kotlin-result.md)(): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[ResponseResult.Success](-success/index.md)&gt;<br>Converts a [ResponseResult](index.md) to a [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) of [ResponseResult.Success](-success/index.md) |