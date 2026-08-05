//[transfer-manager](../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](index.md)/[asMap](as-map.md)

# asMap

[release]\

@[JvmName](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;docItemsToNameSpaces&quot;)

fun [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DocItem](../eu.europa.ec.eudi.iso18013.transfer.response/-doc-item/index.md)&gt;.[asMap](as-map.md)(): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;NameSpace, [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ElementIdentifier&gt;&gt;

Converts a [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of [DocItem](../eu.europa.ec.eudi.iso18013.transfer.response/-doc-item/index.md) to a [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html) of NameSpace to [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of ElementIdentifier

#### Receiver

the [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of [DocItem](../eu.europa.ec.eudi.iso18013.transfer.response/-doc-item/index.md) to convert

#### Return

the [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html) of NameSpace to [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html) of ElementIdentifier