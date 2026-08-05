//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[Outcome](index.md)

# Outcome

[release]\
class [Outcome](index.md)&lt;out [T](index.md)&gt;

Outcome for encapsulating success or failure of a computation for document manager operations. Wraps a [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) instance to provide Java interop.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [isFailure](is-failure.md) | [release]<br>val [isFailure](is-failure.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns `true` if instance represents a failed outcome. In this case [eu.europa.ec.eudi.wallet.document.Outcome.isSuccess](is-success.md) returns `false`. |
| [isSuccess](is-success.md) | [release]<br>val [isSuccess](is-success.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns `true` if instance represents a successful outcome. In this case [eu.europa.ec.eudi.wallet.document.Outcome.isFailure](is-failure.md) return `false` . |
| [kotlinResult](kotlin-result.md) | [release]<br>val [kotlinResult](kotlin-result.md): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[T](index.md)&gt;<br>the [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) instance that this [Outcome](index.md) wraps. |

## Functions

| Name | Summary |
|---|---|
| [exceptionOrNull](exception-or-null.md) | [release]<br>fun [exceptionOrNull](exception-or-null.md)(): [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)?<br>Returns the encapsulated exception if this instance represents a failure outcome or `null` if it is success. |
| [getOrNull](get-or-null.md) | [release]<br>fun [getOrNull](get-or-null.md)(): [T](index.md)?<br>Returns the encapsulated value if this instance represents a successful outcome or `null` if it is failure. |
| [getOrThrow](get-or-throw.md) | [release]<br>fun [getOrThrow](get-or-throw.md)(): [T](index.md)<br>Returns the encapsulated value if this instance represents a successful outcome or throws the encapsulated exception if it is failure. |
| [toString](to-string.md) | [release]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |