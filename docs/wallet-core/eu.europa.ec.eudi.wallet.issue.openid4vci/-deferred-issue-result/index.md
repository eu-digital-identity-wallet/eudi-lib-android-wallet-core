//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DeferredIssueResult](index.md)

# DeferredIssueResult

sealed interface [DeferredIssueResult](index.md) : [OpenId4VciResult](../-open-id4-vci-result/index.md)

Result of a deferred document issuance.

#### Inheritors

| |
|---|
| [DocumentIssued](-document-issued/index.md) |
| [DocumentFailed](-document-failed/index.md) |
| [DocumentNotReady](-document-not-ready/index.md) |
| [DocumentExpired](-document-expired/index.md) |

## Types

| Name | Summary |
|---|---|
| [DocumentExpired](-document-expired/index.md) | [release]<br>data class [DocumentExpired](-document-expired/index.md)(val document: DeferredDocument) : [DeferredIssueResult](index.md), DocumentDetails<br>Document issuance expired. |
| [DocumentFailed](-document-failed/index.md) | [release]<br>data class [DocumentFailed](-document-failed/index.md)(val document: Document, val cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) : [DeferredIssueResult](index.md), DocumentDetails, [OpenId4VciResult.Erroneous](../-open-id4-vci-result/-erroneous/index.md)<br>Document issuance failed. |
| [DocumentIssued](-document-issued/index.md) | [release]<br>data class [DocumentIssued](-document-issued/index.md)(val document: IssuedDocument) : [DeferredIssueResult](index.md), DocumentDetails<br>Document issued successfully. |
| [DocumentNotReady](-document-not-ready/index.md) | [release]<br>data class [DocumentNotReady](-document-not-ready/index.md)(val document: DeferredDocument) : [DeferredIssueResult](index.md), DocumentDetails<br>Document issuance deferred. |

## Properties

| Name | Summary |
|---|---|
| [docType](doc-type.md) | [release]<br>abstract val [docType](doc-type.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the document type |
| [document](document.md) | [release]<br>abstract val [document](document.md): Document |
| [documentId](document-id.md) | [release]<br>abstract val [documentId](document-id.md): DocumentId<br>the id of the document |
| [name](name.md) | [release]<br>abstract val [name](name.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the name of the document |