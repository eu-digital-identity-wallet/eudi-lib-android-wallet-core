//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../index.md)/[MsoMdocItem](index.md)

# MsoMdocItem

[release]\
data class [MsoMdocItem](index.md)(val namespace: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val elementIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)) : [DocItem](../../eu.europa.ec.eudi.iso18013.transfer.response/-doc-item/index.md)

Represents a [DocItem](../../eu.europa.ec.eudi.iso18013.transfer.response/-doc-item/index.md) for mso_mdoc document items. Includes the namespace and element identifier of the document item.

## Constructors

| | |
|---|---|
| [MsoMdocItem](-mso-mdoc-item.md) | [release]<br>constructor(namespace: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), elementIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [elementIdentifier](element-identifier.md) | [release]<br>val [elementIdentifier](element-identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The element identifier of the MsoMdocItem. |
| [namespace](namespace.md) | [release]<br>val [namespace](namespace.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The namespace of the element. |