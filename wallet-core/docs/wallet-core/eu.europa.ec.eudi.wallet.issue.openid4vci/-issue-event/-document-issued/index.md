//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentIssued](index.md)

# DocumentIssued

data class [DocumentIssued](index.md)(val document: IssuedDocument) : [IssueEvent](../index.md), DocumentDetails

Document issued successfully.

#### See also

| | |
|---|---|
| DocumentId | for the document id |

## Constructors

| | |
|---|---|
| [DocumentIssued](-document-issued.md) | [release]<br>constructor(document: IssuedDocument) |

## Properties

| Name | Summary |
|---|---|
| [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F-946843593) | [release]<br>open override val [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [document](document.md) | [release]<br>val [document](document.md): IssuedDocument<br>the issued document |
| [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F-946843593) | [release]<br>open override val [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F-946843593): DocumentId |
| [name](../-document-deferred/index.md#686046743%2FProperties%2F-946843593) | [release]<br>open override val [name](../-document-deferred/index.md#686046743%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |