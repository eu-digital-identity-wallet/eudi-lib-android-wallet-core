//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDocumentByOffer](issue-document-by-offer.md)

# issueDocumentByOffer

[androidJvm]\
abstract fun [issueDocumentByOffer](issue-document-by-offer.md)(offer: [Offer](../-offer/index.md), txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))

Issue a document using an offer

#### Parameters

androidJvm

| | |
|---|---|
| offer | the offer to issue |
| txCode | the transaction code to use for pre-authorized issuing |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called when the document is issued. This callback may be called multiple times, each for every document in the offer |

#### See also

| | |
|---|---|
| [IssueEvent](../-issue-event/index.md) | on how to handle the result |
