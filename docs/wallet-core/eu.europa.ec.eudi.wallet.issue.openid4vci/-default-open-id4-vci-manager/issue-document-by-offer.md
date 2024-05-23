//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DefaultOpenId4VciManager](index.md)/[issueDocumentByOffer](issue-document-by-offer.md)

# issueDocumentByOffer

[androidJvm]\
open override fun [issueDocumentByOffer](issue-document-by-offer.md)(offer: [Offer](../-offer/index.md),
executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?,
onIssueEvent: [OpenId4VciManager.OnIssueEvent](../-open-id4-vci-manager/-on-issue-event/index.md))

Issue a document using an offer

#### Parameters

androidJvm

| | |
|---|---|
| offer | the offer to issue |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called when the document is issued. This callback may be called multiple times, each for every document in the offer |

#### See also

| | |
|---|---|
| [IssueEvent](../-issue-event/index.md) | on how to handle the result |
| [IssueEvent.DocumentRequiresUserAuth](../-issue-event/-document-requires-user-auth/index.md) | on how to handle user authentication |
