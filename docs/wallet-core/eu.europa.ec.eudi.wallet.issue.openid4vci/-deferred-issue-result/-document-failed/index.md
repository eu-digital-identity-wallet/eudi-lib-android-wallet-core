//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[DeferredIssueResult](../index.md)/[DocumentFailed](index.md)

# DocumentFailed

[release]\
data class [DocumentFailed](index.md)(val document: Document, val cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) : [DeferredIssueResult](../index.md), DocumentDetails, [OpenId4VciResult.Erroneous](../../-open-id4-vci-result/-erroneous/index.md)

Document issuance failed.

## Constructors

| | |
|---|---|
| [DocumentFailed](-document-failed.md) | [release]<br>constructor(document: Document, cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [cause](cause.md) | [release]<br>open override val [cause](cause.md): [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)<br>the error that caused the failure |
| [docType](../doc-type.md) | [release]<br>open override val [docType](../doc-type.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the document type |
| [document](document.md) | [release]<br>open override val [document](document.md): Document |
| [documentId](../document-id.md) | [release]<br>open override val [documentId](../document-id.md): DocumentId<br>the id of the document |
| [name](../name.md) | [release]<br>open override val [name](../name.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the name of the document |