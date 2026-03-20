//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[reissueDocument](reissue-document.md)

# reissueDocument

[androidJvm]\
abstract fun [reissueDocument](reissue-document.md)(documentId: &lt;Error class: unknown class&gt;, allowAuthorizationFallback: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = true, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))

Re-issue a previously issued document using stored authorization context.

This method allows re-issuing a credential without requiring the user to go through the full authorization flow again. It uses the stored refresh token to obtain a new access token if needed, and issues a new credential with the same keys.

**Prerequisites:**

- 
   The document must have been issued successfully with a refresh token
- 
   Re-issuance metadata must have been stored during the original issuance
- 
   The refresh token must still be valid (not expired)

**Use Cases:**

- 
   Credential renewal/refresh before expiration
- 
   Updating credential data while maintaining the same keys
- 
   Re-issuing after credential revocation

#### Parameters

androidJvm

| | |
|---|---|
| documentId | the ID of the document to re-issue |
| allowAuthorizationFallback | if `true` (default), falls back to a full OAuth authorization flow when stored tokens are expired. Set to `false` for background re-issuance (e.g. WorkManager) where opening a browser is not possible; in this case, a [eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceAuthorizationException](../../eu.europa.ec.eudi.wallet.issue.openid4vci.reissue/-reissuance-authorization-exception/index.md) is delivered via [IssueEvent.Failure](../-issue-event/-failure/index.md) instead. |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onIssueEvent | the callback to be called during the re-issuance process |

#### See also

| | |
|---|---|
| [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md) | for the possible events during re-issuance |
| [ReissuanceAuthorizationException](../../eu.europa.ec.eudi.wallet.issue.openid4vci.reissue/-reissuance-authorization-exception/index.md) |
