//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[CredentialSetsMatcher](index.md)

# CredentialSetsMatcher

[release]\
class [CredentialSetsMatcher](index.md)

## Constructors

| | |
|---|---|
| [CredentialSetsMatcher](-credential-sets-matcher.md) | [release]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [determineRequestedDocuments](determine-requested-documents.md) | [release]<br>fun [determineRequestedDocuments](determine-requested-documents.md)(credentials: Credentials, credentialSets: CredentialSets?, availableWalletCredentialIds: [Set](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-set/index.html)&lt;QueryId&gt;): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, CredentialQuery&gt;<br>Determines the final map of requested documents based on the DCQL query and the credentials available in the user's wallet. |