//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [DefaultOffer](-default-offer/index.md) | [androidJvm]<br>data class [DefaultOffer](-default-offer/index.md)(credentialOffer: CredentialOffer, filterConfigurations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;CredentialConfigurationFilter&gt; = listOf(FormatFilter, ProofTypeFilter)) : [Offer](-offer/index.md) |
| [DefaultOpenId4VciManager](-default-open-id4-vci-manager/index.md) | [androidJvm]<br>class [DefaultOpenId4VciManager](-default-open-id4-vci-manager/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, var config: [OpenId4VciManager.Config](-open-id4-vci-manager/-config/index.md)) : [OpenId4VciManager](-open-id4-vci-manager/index.md) |
| [IssueEvent](-issue-event/index.md) | [androidJvm]<br>interface [IssueEvent](-issue-event/index.md) |
| [Offer](-offer/index.md) | [androidJvm]<br>interface [Offer](-offer/index.md)<br>An offer of credentials to be issued. |
| [OfferResult](-offer-result/index.md) | [androidJvm]<br>interface [OfferResult](-offer-result/index.md)<br>The result of an offer operation. |
| [OpenId4VciManager](-open-id4-vci-manager/index.md) | [androidJvm]<br>interface [OpenId4VciManager](-open-id4-vci-manager/index.md)<br>OpenId4VciManager is the main entry point to issue documents using the OpenId4Vci protocol It provides methods to issue documents using a document type or an offer, and to resolve an offer |
