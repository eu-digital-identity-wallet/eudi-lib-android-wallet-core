//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [AuthorizationHandler](-authorization-handler/index.md) | [androidJvm]<br>fun interface [AuthorizationHandler](-authorization-handler/index.md)<br>Handler for authorization requests during the OpenID4VCI flow. |
| [AuthorizationResponse](-authorization-response/index.md) | [androidJvm]<br>data class [AuthorizationResponse](-authorization-response/index.md)(val authorizationCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val serverState: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>Response from the authorization flow containing the authorization code and server state. |
| [BatchProofSigner](-batch-proof-signer/index.md) | [androidJvm]<br>class [BatchProofSigner](-batch-proof-signer/index.md)(val signers: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ProofOfPossessionSigner&gt;, keyUnlockData: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), KeyUnlockData?&gt;? = null) : BatchSigner&lt;JwtBindingKey&gt; |
| [BrowserAuthorizationHandler](-browser-authorization-handler/index.md) | [androidJvm]<br>class [BrowserAuthorizationHandler](-browser-authorization-handler/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), logger: [Logger](../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) : [AuthorizationHandler](-authorization-handler/index.md)<br>Default implementation of [AuthorizationHandler](-authorization-handler/index.md) that opens a browser for user authorization. |
| [CreateDocumentSettingsProvider](-create-document-settings-provider/index.md) | [androidJvm]<br>fun interface [CreateDocumentSettingsProvider](-create-document-settings-provider/index.md)<br>The CreateKeySettingsFactory interface is the main entry point to create a new instance of CreateKeySettings required when creating a new org.multipaz.document.Document |
| [DeferredIssueResult](-deferred-issue-result/index.md) | [androidJvm]<br>sealed interface [DeferredIssueResult](-deferred-issue-result/index.md) : [OpenId4VciResult](-open-id4-vci-result/index.md)<br>Result of a deferred document issuance. |
| [IssueEvent](-issue-event/index.md) | [androidJvm]<br>sealed interface [IssueEvent](-issue-event/index.md) : [OpenId4VciResult](-open-id4-vci-result/index.md)<br>Events related to document issuance. |
| [KeyAlias](-key-alias/index.md) | [androidJvm]<br>typealias [KeyAlias](-key-alias/index.md) = [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [KeyAttestationSigner](-key-attestation-signer/index.md) | [androidJvm]<br>class [KeyAttestationSigner](-key-attestation-signer/index.md) : Signer&lt;KeyAttestationJWT&gt; |
| [Offer](-offer/index.md) | [androidJvm]<br>data class [Offer](-offer/index.md)(val credentialOffer: CredentialOffer)<br>Represents an offer of credentials from an issuer. |
| [OfferResult](-offer-result/index.md) | [androidJvm]<br>sealed interface [OfferResult](-offer-result/index.md) : [OpenId4VciResult](-open-id4-vci-result/index.md)<br>The result of an offer operation. |
| [OpenId4VciManager](-open-id4-vci-manager/index.md) | [androidJvm]<br>interface [OpenId4VciManager](-open-id4-vci-manager/index.md)<br>OpenId4VciManager is the main entry point to issue documents using the OpenId4Vci protocol It provides methods to issue documents using a document type or an offer, and to resolve an offer |
| [OpenId4VciResult](-open-id4-vci-result/index.md) | [androidJvm]<br>interface [OpenId4VciResult](-open-id4-vci-result/index.md) |
