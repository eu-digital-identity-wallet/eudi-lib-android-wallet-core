//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDocumentByDocType](issue-document-by-doc-type.md)

# issueDocumentByDocType

[androidJvm]\
abstract fun [~~issueDocumentByDocType~~](issue-document-by-doc-type.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))

---

### Deprecated

Use issueDocumentByConfigurationIdentifier or issueDocumentByFormat instead

---

Issue a document using a document type

#### Parameters

androidJvm

| | |
|---|---|
| docType | the document type to issue |
| txCode | the transaction code to use for pre-authorized issuing |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called when the document is issued |

#### See also

| | |
|---|---|
| [IssueEvent](../-issue-event/index.md) | on how to handle the result |
