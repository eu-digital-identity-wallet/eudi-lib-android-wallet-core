//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[Offer](../index.md)/[OfferedDocument](index.md)

# OfferedDocument

[androidJvm]\
interface [OfferedDocument](index.md)

An item to be issued.

## Functions

| Name                 | Summary                                                                                                                                                                                                                                                                                                                                                            |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [asPair](as-pair.md) | [androidJvm]<br>open fun [asPair](as-pair.md)(): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Converts this item to a pair of name and document type. |

## Properties

| Name                   | Summary                                                                                                                                                                |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [docType](doc-type.md) | [androidJvm]<br>abstract val [docType](doc-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the document type of the item |
| [name](name.md)        | [androidJvm]<br>abstract val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the item                 |
