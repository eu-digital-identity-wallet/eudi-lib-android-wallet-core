//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDocumentByConfigurationIdentifiers](issue-document-by-configuration-identifiers.md)

# issueDocumentByConfigurationIdentifiers

[androidJvm]\
abstract fun [issueDocumentByConfigurationIdentifiers](issue-document-by-configuration-identifiers.md)(credentialConfigurationIds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))

Issue a list of documents using a list of configuration identifiers.

The credential configuration identifier can be obtained from the [getIssuerMetadata](get-issuer-metadata.md)

#### Parameters

androidJvm

| | |
|---|---|
| credentialConfigurationIds | the list of configuration identifiers to issue the document |
| txCode | the transaction code to use for pre-authorized issuing |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called when the document is issued |

#### See also

| | |
|---|---|
| CredentialConfigurationIdentifier | for the configuration identifier |
