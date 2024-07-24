//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DeferredIssueResult](index.md)

# DeferredIssueResult

interface [DeferredIssueResult](index.md) : [OpenId4VciResult](../-open-id4-vci-result/index.md)

#### Inheritors

|                                                  |
|--------------------------------------------------|
| [DocumentIssued](-document-issued/index.md)      |
| [DocumentFailed](-document-failed/index.md)      |
| [DocumentNotReady](-document-not-ready/index.md) |

## Types

| Name                                             | Summary                                                                                                                                                                                                                                                                                                                                                             |
|--------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentFailed](-document-failed/index.md)      | [androidJvm]<br>data class [DocumentFailed](-document-failed/index.md)(val documentId: DocumentId, val cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [DeferredIssueResult](index.md), [OpenId4VciResult.Erroneous](../-open-id4-vci-result/-erroneous/index.md)<br>Document issuance failed.                     |
| [DocumentIssued](-document-issued/index.md)      | [androidJvm]<br>data class [DocumentIssued](-document-issued/index.md)(val documentId: DocumentId, val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [DeferredIssueResult](index.md)<br>Document issued successfully.    |
| [DocumentNotReady](-document-not-ready/index.md) | [androidJvm]<br>data class [DocumentNotReady](-document-not-ready/index.md)(val documentId: DocumentId, val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [DeferredIssueResult](index.md)<br>Document issuance deferred. |
