//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDocumentByConfigurationIdentifier](issue-document-by-configuration-identifier.md)

# issueDocumentByConfigurationIdentifier

[androidJvm]\
abstract fun [issueDocumentByConfigurationIdentifier](issue-document-by-configuration-identifier.md)(credentialConfigurationId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))

Issue a document using a configuration identifier.

The credential configuration identifier can be obtained from the [getIssuerMetadata](get-issuer-metadata.md)

#### Parameters

androidJvm

| | |
|---|---|
| credentialConfigurationId | the configuration identifier to issue the document |
| txCode | the transaction code to use for pre-authorized issuing |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called when the document is issued |

#### See also

| | |
|---|---|
| CredentialConfigurationIdentifier | for the configuration identifier |
