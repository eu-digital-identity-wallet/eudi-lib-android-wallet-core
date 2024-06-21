//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDeferredDocument](issue-deferred-document.md)

# issueDeferredDocument

[androidJvm]\
abstract fun [issueDeferredDocument](issue-deferred-document.md)(deferredDocument: DeferredDocument,
executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?,
onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))

Issue a deferred document

#### Parameters

androidJvm

|                  |                                                                                                                               |
|------------------|-------------------------------------------------------------------------------------------------------------------------------|
| deferredDocument | the deferred document to issue                                                                                                |
| executor         | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent     | the callback to be called when the document is issued                                                                         |

#### See also

|                                        |                             |
|----------------------------------------|-----------------------------|
| [IssueEvent](../-issue-event/index.md) | on how to handle the result |
