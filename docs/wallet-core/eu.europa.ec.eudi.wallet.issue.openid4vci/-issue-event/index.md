//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[IssueEvent](index.md)

# IssueEvent

sealed interface [IssueEvent](index.md) : [OpenId4VciResult](../-open-id4-vci-result/index.md)

Events related to document issuance.

#### Inheritors

| |
|---|
| [Started](-started/index.md) |
| [Finished](-finished/index.md) |
| [Failure](-failure/index.md) |
| [DocumentIssued](-document-issued/index.md) |
| [DocumentFailed](-document-failed/index.md) |
| [DocumentRequiresCreateSettings](-document-requires-create-settings/index.md) |
| [DocumentRequiresUserAuth](-document-requires-user-auth/index.md) |
| [DocumentDeferred](-document-deferred/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |
| [DocumentDeferred](-document-deferred/index.md) | [androidJvm]<br>data class [DocumentDeferred](-document-deferred/index.md)(val document: DeferredDocument) : [IssueEvent](index.md), DocumentDetails<br>Document issuance deferred. |
| [DocumentFailed](-document-failed/index.md) | [androidJvm]<br>data class [DocumentFailed](-document-failed/index.md)(document: UnsignedDocument, val cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)) : [IssueEvent](index.md), DocumentDetails, [OpenId4VciResult.Erroneous](../-open-id4-vci-result/-erroneous/index.md)<br>Document issuance failed. |
| [DocumentIssued](-document-issued/index.md) | [androidJvm]<br>data class [DocumentIssued](-document-issued/index.md)(val document: IssuedDocument) : [IssueEvent](index.md), DocumentDetails<br>Document issued successfully. |
| [DocumentRequiresCreateSettings](-document-requires-create-settings/index.md) | [androidJvm]<br>data class [DocumentRequiresCreateSettings](-document-requires-create-settings/index.md)(val offeredDocument: [Offer.OfferedDocument](../-offer/-offered-document/index.md), val resume: (createDocumentSettings: CreateDocumentSettings) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html), val cancel: (reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)) : [IssueEvent](index.md)<br>Issuing requires CreateDocumentSettings to create the document that will be issued for the [offeredDocument](-document-requires-create-settings/offered-document.md). |
| [DocumentRequiresUserAuth](-document-requires-user-auth/index.md) | [androidJvm]<br>data class [DocumentRequiresUserAuth](-document-requires-user-auth/index.md)(val document: UnsignedDocument, val signingAlgorithm: Algorithm, val keysRequireAuth: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../-key-alias/index.md), SecureArea&gt;, val resume: (keyUnlockData: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../-key-alias/index.md), KeyUnlockData?&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html), val cancel: (reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)) : [IssueEvent](index.md), DocumentDetails<br>Document requires user authentication to unlock the key for signing the proof of possession. |
| [Failure](-failure/index.md) | [androidJvm]<br>data class [Failure](-failure/index.md)(val cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)) : [IssueEvent](index.md), [OpenId4VciResult.Erroneous](../-open-id4-vci-result/-erroneous/index.md)<br>The issuance failed. |
| [Finished](-finished/index.md) | [androidJvm]<br>data class [Finished](-finished/index.md)(val issuedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;) : [IssueEvent](index.md)<br>The issuance has finished. |
| [Started](-started/index.md) | [androidJvm]<br>data class [Started](-started/index.md)(val total: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) : [IssueEvent](index.md)<br>The issuance was started. |
