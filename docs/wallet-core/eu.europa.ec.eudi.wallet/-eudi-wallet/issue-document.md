//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[issueDocument](issue-document.md)

# issueDocument

[androidJvm]\

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)

fun [issueDocument](issue-document.md)(
docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? =
null,
callback: [OpenId4VciManager.OnIssueCallback](../../eu.europa.ec.eudi.wallet.document.issue.openid4vci/-open-id4-vci-manager/-on-issue-callback/index.md))

Issue a document with the given [docType](issue-document.md) using OpenId4Vci protocol

Example:

```kotlin
EudiWallet.issueDocument("eu.europa.ec.eudiw.pid.1", mainExecutor) { result ->
   when (result) {
     is IssueDocumentResult.Success -> {
       // document issued successfully
     }
     is IssueDocumentResult.Failure -> {
       // document issued failed
     }
     is IssueDocumentResult.UserAuthRequired -> {
       // user authentication is required
     }
   }
 }
```

#### Parameters

androidJvm

|          |                                                                                                                               |
|----------|-------------------------------------------------------------------------------------------------------------------------------|
| docType  | the docType of the document                                                                                                   |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| callback | the callback to be called when the document is issued                                                                         |

#### See also

|                                                                                                                                     |
|-------------------------------------------------------------------------------------------------------------------------------------|
| [OpenId4VciManager.issueDocument](../../eu.europa.ec.eudi.wallet.document.issue.openid4vci/-open-id4-vci-manager/issue-document.md) |

#### Throws

|                                                                                                                  |                                                                                                                                                                                             |
|------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method or if the [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md) is not set |
