//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[DeferredIssueResult](../index.md)/[DocumentFailed](index.md)

# DocumentFailed

[androidJvm]\
data class [DocumentFailed](index.md)(val documentId: DocumentId, val
name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [DeferredIssueResult](../index.md), [OpenId4VciResult.Erroneous](../../-open-id4-vci-result/-erroneous/index.md)

Document issuance failed.

## Constructors

|                                       |                                                                                                                                                                                                                                                                                                                                       |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentFailed](-document-failed.md) | [androidJvm]<br>constructor(documentId: DocumentId, name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name                         | Summary                                                                                                                                                                          |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [cause](cause.md)            | [androidJvm]<br>open override val [cause](cause.md): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)<br>the error that caused the failure |
| [docType](doc-type.md)       | [androidJvm]<br>open override val [docType](doc-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the document type                  |
| [documentId](document-id.md) | [androidJvm]<br>open override val [documentId](document-id.md): DocumentId<br>the id of the failed document                                                                      |
| [name](name.md)              | [androidJvm]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the document                  |
