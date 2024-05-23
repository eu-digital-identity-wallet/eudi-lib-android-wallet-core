//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DefaultOffer](index.md)

# DefaultOffer

[androidJvm]\
data class [DefaultOffer](index.md)(credentialOffer: CredentialOffer, filterConfigurations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;CredentialConfigurationFilter&gt; = listOf(FormatFilter, ProofTypeFilter)) : [Offer](../-offer/index.md)

## Constructors

| | |
|---|---|
| [DefaultOffer](-default-offer.md) | [androidJvm]<br>constructor(credentialOffer: CredentialOffer, filterConfigurations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;CredentialConfigurationFilter&gt; = listOf(FormatFilter, ProofTypeFilter)) |

## Properties

| Name | Summary |
|---|---|
| [issuerName](issuer-name.md) | [androidJvm]<br>open override val [issuerName](issuer-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the issuer |
| [offeredDocuments](offered-documents.md) | [androidJvm]<br>open override val [offeredDocuments](offered-documents.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Offer.OfferedDocument](../-offer/-offered-document/index.md)&gt;<br>the items to be issued |
