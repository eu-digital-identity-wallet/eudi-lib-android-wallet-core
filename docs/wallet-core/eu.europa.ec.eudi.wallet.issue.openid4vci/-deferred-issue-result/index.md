//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DeferredIssueResult](index.md)

# DeferredIssueResult

interface [DeferredIssueResult](index.md) : [OpenId4VciResult](../-open-id4-vci-result/index.md)

Result of a deferred document issuance.

#### Inheritors

|                                                  |
|--------------------------------------------------|
| [DocumentIssued](-document-issued/index.md)      |
| [DocumentFailed](-document-failed/index.md)      |
| [DocumentNotReady](-document-not-ready/index.md) |
| [DocumentExpired](-document-expired/index.md)    |

## Types

| Name                                             | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentExpired](-document-expired/index.md)    | [androidJvm]<br>data class [DocumentExpired](-document-expired/index.md)(val documentId: DocumentId, val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [DeferredIssueResult](index.md)<br>Document issuance expired.                                                                                                                                                                            |
| [DocumentFailed](-document-failed/index.md)      | [androidJvm]<br>data class [DocumentFailed](-document-failed/index.md)(val documentId: DocumentId, val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [DeferredIssueResult](index.md), [OpenId4VciResult.Erroneous](../-open-id4-vci-result/-erroneous/index.md)<br>Document issuance failed. |
| [DocumentIssued](-document-issued/index.md)      | [androidJvm]<br>data class [DocumentIssued](-document-issued/index.md)(val documentId: DocumentId, val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [DeferredIssueResult](index.md)<br>Document issued successfully.                                                                                                                                                                           |
| [DocumentNotReady](-document-not-ready/index.md) | [androidJvm]<br>data class [DocumentNotReady](-document-not-ready/index.md)(val documentId: DocumentId, val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [DeferredIssueResult](index.md)<br>Document issuance deferred.                                                                                                                                                                        |

## Properties

| Name                         | Summary                                                                                                                                                    |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [docType](doc-type.md)       | [androidJvm]<br>abstract val [docType](doc-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the document type |
| [documentId](document-id.md) | [androidJvm]<br>abstract val [documentId](document-id.md): DocumentId<br>the id of the document                                                            |
| [name](name.md)              | [androidJvm]<br>abstract val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the document |
