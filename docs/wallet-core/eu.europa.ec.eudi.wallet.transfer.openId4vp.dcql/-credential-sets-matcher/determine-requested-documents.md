//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[CredentialSetsMatcher](index.md)/[determineRequestedDocuments](determine-requested-documents.md)

# determineRequestedDocuments

[androidJvm]\
fun [determineRequestedDocuments](determine-requested-documents.md)(credentials: Credentials, credentialSets: CredentialSets?, availableWalletCredentialIds: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-set/index.html)&lt;QueryId&gt;): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, CredentialQuery&gt;

Determines the final map of requested documents based on the DCQL query and the credentials available in the user's wallet.

#### Return

A map of QueryId to the corresponding CredentialQuery for documents that should be presented.

- 
   If all required credential sets can be satisfied, it returns the required documents plus any optional ones that can also be satisfied.
- 
   If any required set cannot be satisfied, it returns an empty map.
- 
   If 'credential_sets' is not present, it returns all credentials from the query that are available in the wallet.

#### Parameters

androidJvm

| | |
|---|---|
| credentials | The list of all possible credentials defined in the request. |
| credentialSets | The credential_sets rules from the request, which may be null. |
| availableWalletCredentialIds | A set of QueryIds for credentials the wallet actually has. |
