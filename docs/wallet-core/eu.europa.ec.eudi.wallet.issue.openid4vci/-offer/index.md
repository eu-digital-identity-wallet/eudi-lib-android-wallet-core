//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[Offer](index.md)

# Offer

[androidJvm]\
data class [Offer](index.md)(val credentialOffer: CredentialOffer)

Represents an offer of credentials from an issuer.

## Constructors

|                    |                                                               |
|--------------------|---------------------------------------------------------------|
| [Offer](-offer.md) | [androidJvm]<br>constructor(credentialOffer: CredentialOffer) |

## Types

| Name                                          | Summary                                                                                                                                                                                                                                                                            |
|-----------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [OfferedDocument](-offered-document/index.md) | [androidJvm]<br>data class [OfferedDocument](-offered-document/index.md)(val offer: [Offer](index.md), val configurationIdentifier: CredentialConfigurationIdentifier, val configuration: CredentialConfiguration)<br>Represents an offered document part of an [Offer](index.md). |

## Properties

| Name                                     | Summary                                                                                                                                                                                                                                |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [credentialOffer](credential-offer.md)   | [androidJvm]<br>val [credentialOffer](credential-offer.md): CredentialOffer<br>credential offer                                                                                                                                        |
| [issuerMetadata](issuer-metadata.md)     | [androidJvm]<br>val [issuerMetadata](issuer-metadata.md): CredentialIssuerMetadata<br>issuer metadata                                                                                                                                  |
| [offeredDocuments](offered-documents.md) | [androidJvm]<br>val [offeredDocuments](offered-documents.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Offer.OfferedDocument](-offered-document/index.md)&gt;<br>offered documents |
| [txCodeSpec](tx-code-spec.md)            | [androidJvm]<br>val [txCodeSpec](tx-code-spec.md): TxCode?<br>offered documents                                                                                                                                                        |
