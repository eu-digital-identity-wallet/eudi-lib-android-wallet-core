//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[RequestedDocuments](index.md)

# RequestedDocuments

class [RequestedDocuments](index.md)(documents: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt;) : [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt; 

Wrapper class that contains the requested documents

#### Parameters

release

| | |
|---|---|
| documents | the list of requested documents |

## Constructors

| | |
|---|---|
| [RequestedDocuments](-requested-documents.md) | [release]<br>constructor(documents: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt;)constructor(vararg documents: [RequestedDocument](../-requested-document/index.md))<br>Constructor that takes a vararg of [RequestedDocument](../-requested-document/index.md) and converts it to a list |

## Properties

| Name | Summary |
|---|---|
| [size](index.md#844915858%2FProperties%2F1715836493) | [release]<br>open override val [size](index.md#844915858%2FProperties%2F1715836493): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [asMap](../../eu.europa.ec.eudi.iso18013.transfer/as-map.md) | [release]<br>@[JvmName](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;docItemsToNameSpaces&quot;)<br>fun [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;.[asMap](../../eu.europa.ec.eudi.iso18013.transfer/as-map.md)(): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;NameSpace, [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ElementIdentifier&gt;&gt;<br>Converts a [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of [DocItem](../-doc-item/index.md) to a [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html) of NameSpace to [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of ElementIdentifier |
| [contains](index.md#765883978%2FFunctions%2F1715836493) | [release]<br>open operator override fun [contains](index.md#765883978%2FFunctions%2F1715836493)(element: [RequestedDocument](../-requested-document/index.md)): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [containsAll](index.md#-225903147%2FFunctions%2F1715836493) | [release]<br>open override fun [containsAll](index.md#-225903147%2FFunctions%2F1715836493)(elements: [Collection](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt;): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [get](index.md#961975567%2FFunctions%2F1715836493) | [release]<br>open operator override fun [get](index.md#961975567%2FFunctions%2F1715836493)(index: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [RequestedDocument](../-requested-document/index.md) |
| [indexOf](index.md#-407930336%2FFunctions%2F1715836493) | [release]<br>open override fun [indexOf](index.md#-407930336%2FFunctions%2F1715836493)(element: [RequestedDocument](../-requested-document/index.md)): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |
| [isEmpty](index.md#-1000881820%2FFunctions%2F1715836493) | [release]<br>open override fun [isEmpty](index.md#-1000881820%2FFunctions%2F1715836493)(): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [iterator](index.md#-1577986619%2FFunctions%2F1715836493) | [release]<br>open operator override fun [iterator](index.md#-1577986619%2FFunctions%2F1715836493)(): [Iterator](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-iterator/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt; |
| [lastIndexOf](index.md#1327716778%2FFunctions%2F1715836493) | [release]<br>open override fun [lastIndexOf](index.md#1327716778%2FFunctions%2F1715836493)(element: [RequestedDocument](../-requested-document/index.md)): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |
| [listIterator](index.md#-236165689%2FFunctions%2F1715836493) | [release]<br>open override fun [listIterator](index.md#-236165689%2FFunctions%2F1715836493)(): [ListIterator](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list-iterator/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt;<br>open override fun [listIterator](index.md#845091493%2FFunctions%2F1715836493)(index: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [ListIterator](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list-iterator/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt; |
| [subList](index.md#423386006%2FFunctions%2F1715836493) | [release]<br>open override fun [subList](index.md#423386006%2FFunctions%2F1715836493)(fromIndex: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html), toIndex: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[RequestedDocument](../-requested-document/index.md)&gt; |