//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DefaultOpenId4VciManager](index.md)/[issueDocumentByOfferUri](issue-document-by-offer-uri.md)

# issueDocumentByOfferUri

[androidJvm]\
open override fun [issueDocumentByOfferUri](issue-document-by-offer-uri.md)(
offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?,
onIssueEvent: [OpenId4VciManager.OnIssueEvent](../-open-id4-vci-manager/-on-issue-event/index.md))

Issue a document using an offer URI

#### Parameters

androidJvm

| | |
|---|---|
| offerUri | the offer URI |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called when the document is issued. This callback may be called multiple times, each for every document in the offer |

#### See also

| | |
|---|---|
| [IssueEvent](../-issue-event/index.md) | on how to handle the result |
| [IssueEvent.DocumentRequiresUserAuth](../-issue-event/-document-requires-user-auth/index.md) | on how to handle user authentication |
