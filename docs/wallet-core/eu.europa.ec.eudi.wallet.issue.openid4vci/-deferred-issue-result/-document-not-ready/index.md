//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[DeferredIssueResult](../index.md)/[DocumentNotReady](index.md)

# DocumentNotReady

[androidJvm]\
data class [DocumentNotReady](index.md)(val document:
DeferredDocument) : [DeferredIssueResult](../index.md), DocumentDetails

Document issuance deferred.

## Constructors

|                                            |                                                         |
|--------------------------------------------|---------------------------------------------------------|
| [DocumentNotReady](-document-not-ready.md) | [androidJvm]<br>constructor(document: DeferredDocument) |

## Properties

| Name                            | Summary                                                                                                                                                            |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [docType](../doc-type.md)       | [androidJvm]<br>open override val [docType](../doc-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the document type |
| [document](document.md)         | [androidJvm]<br>open override val [document](document.md): DeferredDocument                                                                                        |
| [documentId](../document-id.md) | [androidJvm]<br>open override val [documentId](../document-id.md): DocumentId<br>the id of the document                                                            |
| [name](../name.md)              | [androidJvm]<br>open override val [name](../name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the document |
