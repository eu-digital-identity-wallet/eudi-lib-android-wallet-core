//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[DisclosedDocuments](index.md)

# DisclosedDocuments

class [DisclosedDocuments](index.md)(documents: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt; = emptyList()) : [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt; 

The list of disclosed documents, the documents and their elements that the holder has disclosed to the verifier.

#### Parameters

release

| | |
|---|---|
| documents | the list of disclosed documents |

## Constructors

| | |
|---|---|
| [DisclosedDocuments](-disclosed-documents.md) | [release]<br>constructor(documents: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt; = emptyList())constructor(vararg documents: [DisclosedDocument](../-disclosed-document/index.md))<br>Constructor for vararg DisclosedDocument parameters |

## Properties

| Name | Summary |
|---|---|
| [size](../-requested-documents/index.md#844915858%2FProperties%2F1715836493) | [release]<br>open override val [size](../-requested-documents/index.md#844915858%2FProperties%2F1715836493): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [asMap](../../eu.europa.ec.eudi.iso18013.transfer/as-map.md) | [release]<br>@[JvmName](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;docItemsToNameSpaces&quot;)<br>fun [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../-doc-item/index.md)&gt;.[asMap](../../eu.europa.ec.eudi.iso18013.transfer/as-map.md)(): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;NameSpace, [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ElementIdentifier&gt;&gt;<br>Converts a [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of [DocItem](../-doc-item/index.md) to a [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html) of NameSpace to [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of ElementIdentifier |
| [contains](../-requested-documents/index.md#765883978%2FFunctions%2F1715836493) | [release]<br>open operator override fun [contains](../-requested-documents/index.md#765883978%2FFunctions%2F1715836493)(element: [DisclosedDocument](../-disclosed-document/index.md)): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [containsAll](../-requested-documents/index.md#-225903147%2FFunctions%2F1715836493) | [release]<br>open override fun [containsAll](../-requested-documents/index.md#-225903147%2FFunctions%2F1715836493)(elements: [Collection](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-collection/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt;): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [get](../-requested-documents/index.md#961975567%2FFunctions%2F1715836493) | [release]<br>open operator override fun [get](../-requested-documents/index.md#961975567%2FFunctions%2F1715836493)(index: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [DisclosedDocument](../-disclosed-document/index.md) |
| [indexOf](../-requested-documents/index.md#-407930336%2FFunctions%2F1715836493) | [release]<br>open override fun [indexOf](../-requested-documents/index.md#-407930336%2FFunctions%2F1715836493)(element: [DisclosedDocument](../-disclosed-document/index.md)): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |
| [isEmpty](../-requested-documents/index.md#-1000881820%2FFunctions%2F1715836493) | [release]<br>open override fun [isEmpty](../-requested-documents/index.md#-1000881820%2FFunctions%2F1715836493)(): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [iterator](../-requested-documents/index.md#-1577986619%2FFunctions%2F1715836493) | [release]<br>open operator override fun [iterator](../-requested-documents/index.md#-1577986619%2FFunctions%2F1715836493)(): [Iterator](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-iterator/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt; |
| [lastIndexOf](../-requested-documents/index.md#1327716778%2FFunctions%2F1715836493) | [release]<br>open override fun [lastIndexOf](../-requested-documents/index.md#1327716778%2FFunctions%2F1715836493)(element: [DisclosedDocument](../-disclosed-document/index.md)): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |
| [listIterator](../-requested-documents/index.md#-236165689%2FFunctions%2F1715836493) | [release]<br>open override fun [listIterator](../-requested-documents/index.md#-236165689%2FFunctions%2F1715836493)(): [ListIterator](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list-iterator/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt;<br>open override fun [listIterator](../-requested-documents/index.md#845091493%2FFunctions%2F1715836493)(index: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [ListIterator](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list-iterator/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt; |
| [subList](../-requested-documents/index.md#423386006%2FFunctions%2F1715836493) | [release]<br>open override fun [subList](../-requested-documents/index.md#423386006%2FFunctions%2F1715836493)(fromIndex: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html), toIndex: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DisclosedDocument](../-disclosed-document/index.md)&gt; |