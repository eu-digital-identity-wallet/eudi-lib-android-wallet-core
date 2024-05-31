//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDocumentByDocType](issue-document-by-doc-type.md)

# issueDocumentByDocType

[androidJvm]\
abstract fun [issueDocumentByDocType](issue-document-by-doc-type.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))

Issue a document using a document type

#### Parameters

androidJvm

| | |
|---|---|
| docType | the document type to issue |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called when the document is issued |

#### See also

| | |
|---|---|
| [IssueEvent](../-issue-event/index.md) | on how to handle the result |
| [IssueEvent.DocumentRequiresUserAuth](../-issue-event/-document-requires-user-auth/index.md) | on how to handle user authentication |
