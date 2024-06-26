//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[Offer](index.md)

# Offer

[androidJvm]\
interface [Offer](index.md)

An offer of credentials to be issued.

## Types

| Name                                          | Summary                                                                                                                                                                                                                                                                                                                                                                                                           |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [OfferedDocument](-offered-document/index.md) | [androidJvm]<br>interface [OfferedDocument](-offered-document/index.md)<br>An item to be issued.                                                                                                                                                                                                                                                                                                                  |
| [TxCodeSpec](-tx-code-spec/index.md)          | [androidJvm]<br>data class [TxCodeSpec](-tx-code-spec/index.md)(val inputMode: [Offer.TxCodeSpec.InputMode](-tx-code-spec/-input-mode/index.md) = InputMode.NUMERIC, val length: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?, val description: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null)<br>Specification for a transaction code. |

## Properties

| Name                                     | Summary                                                                                                                                                                                                                                              |
|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [issuerName](issuer-name.md)             | [androidJvm]<br>abstract val [issuerName](issuer-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the issuer                                                                                |
| [offeredDocuments](offered-documents.md) | [androidJvm]<br>abstract val [offeredDocuments](offered-documents.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Offer.OfferedDocument](-offered-document/index.md)&gt;<br>the items to be issued |
| [txCodeSpec](tx-code-spec.md)            | [androidJvm]<br>abstract val [txCodeSpec](tx-code-spec.md): [Offer.TxCodeSpec](-tx-code-spec/index.md)?<br>the specification for the transaction code                                                                                                |
