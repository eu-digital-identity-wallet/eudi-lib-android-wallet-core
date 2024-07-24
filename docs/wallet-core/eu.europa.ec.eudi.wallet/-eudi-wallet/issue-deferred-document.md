//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[issueDeferredDocument](issue-deferred-document.md)

# issueDeferredDocument

[androidJvm]\
fun [~~issueDeferredDocument~~](issue-deferred-document.md)(documentId: DocumentId,
executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null,
onResult: [OpenId4VciManager.OnDeferredIssueResult](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-deferred-issue-result/index.md))

---

### Deprecated

Use EudiWallet.createOpenId4VciManager() to create an instance of OpenId4VciManager and use the
OpendId4VciManager.issueDeferredDocument() instead

---

Issue a deferred document using the OpenId4VCI protocol

#### Parameters

androidJvm

|            |                                                                                                                               |
|------------|-------------------------------------------------------------------------------------------------------------------------------|
| documentId | the id of the deferred document                                                                                               |
| executor   | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onResult   | the callback to be called when the document is issued                                                                         |

#### See also

|                                                                                                                                                     |
|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| [OpenId4VciManager.issueDeferredDocument](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/issue-deferred-document.md)         |
| [OpenId4VciManager.OnDeferredIssueResult](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-deferred-issue-result/index.md) | on how to handle the result |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
