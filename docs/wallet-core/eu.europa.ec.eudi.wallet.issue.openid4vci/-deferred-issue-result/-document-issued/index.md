//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[DeferredIssueResult](../index.md)/[DocumentIssued](index.md)

# DocumentIssued

data class [DocumentIssued](index.md)(val document: IssuedDocument) : [DeferredIssueResult](../index.md), DocumentDetails

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
| [docType](../doc-type.md) | [release]<br>open override val [docType](../doc-type.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the document type |
| [document](document.md) | [release]<br>open override val [document](document.md): IssuedDocument |
| [documentId](../document-id.md) | [release]<br>open override val [documentId](../document-id.md): DocumentId<br>the id of the document |
| [name](../name.md) | [release]<br>open override val [name](../name.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the name of the document |