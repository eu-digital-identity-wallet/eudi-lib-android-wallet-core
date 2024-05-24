//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[Offer](index.md)

# Offer

[androidJvm]\
interface [Offer](index.md)

An offer of credentials to be issued.

## Types

| Name | Summary |
|---|---|
| [OfferedDocument](-offered-document/index.md) | [androidJvm]<br>data class [OfferedDocument](-offered-document/index.md)<br>An item to be issued. |

## Properties

| Name | Summary |
|---|---|
| [issuerName](issuer-name.md) | [androidJvm]<br>abstract val [issuerName](issuer-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the issuer |
| [offeredDocuments](offered-documents.md) | [androidJvm]<br>abstract val [offeredDocuments](offered-documents.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Offer.OfferedDocument](-offered-document/index.md)&gt;<br>the items to be issued |
