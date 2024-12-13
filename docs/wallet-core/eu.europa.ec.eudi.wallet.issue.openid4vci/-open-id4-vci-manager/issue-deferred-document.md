//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDeferredDocument](issue-deferred-document.md)

# issueDeferredDocument

[androidJvm]\
abstract fun [issueDeferredDocument](issue-deferred-document.md)(deferredDocument: DeferredDocument, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueResult: [OpenId4VciManager.OnDeferredIssueResult](-on-deferred-issue-result/index.md))

Issue a deferred document

#### Parameters

androidJvm

| | |
|---|---|
| deferredDocument | the deferred document to issue |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueResult | the callback to be called when the document is issued |

#### See also

| | |
|---|---|
| [OpenId4VciManager.OnDeferredIssueResult](-on-deferred-issue-result/index.md) | on how to handle the result |
