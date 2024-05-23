//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[issueDocument](issue-document.md)

# issueDocument

[androidJvm]\
fun [~~issueDocument~~](issue-document.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onEvent: [OpenId4VciManager.OnIssueEvent](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-issue-event/index.md))

---

### Deprecated

Use issueDocumentByDocType instead

#### Replace with

```kotlin
issueDocumentByDocType(docType, executor, onResult)
```
---

Issue a document using the OpenId4VCI protocol

#### Parameters

androidJvm

| | |
|---|---|
| docType | the document type to issue |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onEvent | the callback to be called when the document is issued |

#### See also

| |
|---|
| [OpenId4VciManager.issueDocumentByDocType](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/issue-document-by-doc-type.md) |
| [OpenId4VciManager.OnIssueEvent](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-issue-event/index.md) | on how to handle the result |
| [IssueEvent.DocumentRequiresUserAuth](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-issue-event/-document-requires-user-auth/index.md) | on how to handle user authentication |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md) is not set |
