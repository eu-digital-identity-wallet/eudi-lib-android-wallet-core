//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document](../../index.md)/[DocumentManager](../index.md)/[Companion](index.md)/[invoke](invoke.md)

# invoke

[release]\

@[JvmStatic](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

operator fun [invoke](invoke.md)(configure: [DocumentManager.Builder](../-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)): [DocumentManager](../index.md)

Create a [DocumentManager](../index.md) instance.

#### Return

the document manager

#### Parameters

release

| | |
|---|---|
| configure | the builder configuration |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if the storage engine or secure area is not set |