//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentIssued](index.md)

# DocumentIssued

data class [DocumentIssued](index.md)(val document: IssuedDocument) : [IssueEvent](../index.md),
DocumentDetails

Document issued successfully.

#### See also

|            |                     |
|------------|---------------------|
| DocumentId | for the document id |

## Constructors

|                                       |                                                       |
|---------------------------------------|-------------------------------------------------------|
| [DocumentIssued](-document-issued.md) | [androidJvm]<br>constructor(document: IssuedDocument) |

## Properties

| Name                                                                              | Summary                                                                                                                                                                                             |
|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F1615067946)   | [androidJvm]<br>open override val [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [document](document.md)                                                           | [androidJvm]<br>val [document](document.md): IssuedDocument<br>the issued document                                                                                                                  |
| [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F1615067946) | [androidJvm]<br>open override val [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F1615067946): DocumentId                                                                     |
| [name](../-document-deferred/index.md#686046743%2FProperties%2F1615067946)        | [androidJvm]<br>open override val [name](../-document-deferred/index.md#686046743%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)      |
