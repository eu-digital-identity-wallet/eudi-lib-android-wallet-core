//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentRequiresCreateSettings](index.md)

# DocumentRequiresCreateSettings

[release]\
data class [DocumentRequiresCreateSettings](index.md)(val offeredDocument: [Offer.OfferedDocument](../../-offer/-offered-document/index.md), val resume: (createDocumentSettings: CreateDocumentSettings) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html), val cancel: (reason: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)) : [IssueEvent](../index.md)

Issuing requires CreateDocumentSettings to create the document that will be issued for the offeredDocument.

## Constructors

| | |
|---|---|
| [DocumentRequiresCreateSettings](-document-requires-create-settings.md) | [release]<br>constructor(offeredDocument: [Offer.OfferedDocument](../../-offer/-offered-document/index.md), resume: (createDocumentSettings: CreateDocumentSettings) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html), cancel: (reason: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [release]<br>val [cancel](cancel.md): (reason: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)<br>the callback to cancel the issuance with an optional reason |
| [offeredDocument](offered-document.md) | [release]<br>val [offeredDocument](offered-document.md): [Offer.OfferedDocument](../../-offer/-offered-document/index.md)<br>the offered document |
| [resume](resume.md) | [release]<br>val [resume](resume.md): (createDocumentSettings: CreateDocumentSettings) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)<br>the callback to resume the issuance with the CreateDocumentSettings that will be used to create the document |