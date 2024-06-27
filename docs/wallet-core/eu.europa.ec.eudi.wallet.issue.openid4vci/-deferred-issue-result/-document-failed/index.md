//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[DeferredIssueResult](../index.md)/[DocumentFailed](index.md)

# DocumentFailed

[androidJvm]\
data class [DocumentFailed](index.md)(val documentId: DocumentId, val
cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [DeferredIssueResult](../index.md), [OpenId4VciResult.Erroneous](../../-open-id4-vci-result/-erroneous/index.md)

Document issuance failed.

## Constructors

|                                       |                                                                                                                                                    |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentFailed](-document-failed.md) | [androidJvm]<br>constructor(documentId: DocumentId, cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name                         | Summary                                                                                                                                                                          |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [cause](cause.md)            | [androidJvm]<br>open override val [cause](cause.md): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)<br>the error that caused the failure |
| [documentId](document-id.md) | [androidJvm]<br>val [documentId](document-id.md): DocumentId                                                                                                                     |
