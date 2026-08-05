//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentFailed](index.md)

# DocumentFailed

[release]\
data class [DocumentFailed](index.md)(document: UnsignedDocument, val cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) : [IssueEvent](../index.md), DocumentDetails, [OpenId4VciResult.Erroneous](../../-open-id4-vci-result/-erroneous/index.md)

Document issuance failed.

## Constructors

| | |
|---|---|
| [DocumentFailed](-document-failed.md) | [release]<br>constructor(document: UnsignedDocument, cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [cause](cause.md) | [release]<br>open override val [cause](cause.md): [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)<br>the error that caused the failure |
| [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F-946843593) | [release]<br>open override val [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F-946843593) | [release]<br>open override val [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F-946843593): DocumentId |
| [name](../-document-deferred/index.md#686046743%2FProperties%2F-946843593) | [release]<br>open override val [name](../-document-deferred/index.md#686046743%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |