//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentRequiresCreateSettings](index.md)

# DocumentRequiresCreateSettings

[androidJvm]\
data class [DocumentRequiresCreateSettings](index.md)(val
offeredDocument: [Offer.OfferedDocument](../../-offer/-offered-document/index.md), val resume: (
createDocumentSettings: CreateDocumentSettings)
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), val cancel: (
reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?)
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [IssueEvent](../index.md)

Issuing requires CreateDocumentSettings to create the document that will be issued for
the [offeredDocument](offered-document.md).

## Constructors

|                                                                         |                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentRequiresCreateSettings](-document-requires-create-settings.md) | [androidJvm]<br>constructor(offeredDocument: [Offer.OfferedDocument](../../-offer/-offered-document/index.md), resume: (createDocumentSettings: CreateDocumentSettings) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), cancel: (reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |

## Properties

| Name                                   | Summary                                                                                                                                                                                                                                                                                      |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [cancel](cancel.md)                    | [androidJvm]<br>val [cancel](cancel.md): (reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>the callback to cancel the issuance with an optional reason       |
| [offeredDocument](offered-document.md) | [androidJvm]<br>val [offeredDocument](offered-document.md): [Offer.OfferedDocument](../../-offer/-offered-document/index.md)<br>the offered document                                                                                                                                         |
| [resume](resume.md)                    | [androidJvm]<br>val [resume](resume.md): (createDocumentSettings: CreateDocumentSettings) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>the callback to resume the issuance with the CreateDocumentSettings that will be used to create the document |
