//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[Offer](index.md)

# Offer

[release]\
data class [Offer](index.md)(val credentialOffer: CredentialOffer)

Represents an offer of credentials from an issuer.

## Constructors

| | |
|---|---|
| [Offer](-offer.md) | [release]<br>constructor(credentialOffer: CredentialOffer) |

## Types

| Name | Summary |
|---|---|
| [OfferedDocument](-offered-document/index.md) | [release]<br>data class [OfferedDocument](-offered-document/index.md)(val offer: [Offer](index.md), val configurationIdentifier: CredentialConfigurationIdentifier, val configuration: CredentialConfiguration)<br>Represents an offered document part of an [Offer](index.md). |

## Properties

| Name | Summary |
|---|---|
| [credentialOffer](credential-offer.md) | [release]<br>val [credentialOffer](credential-offer.md): CredentialOffer<br>credential offer |
| [issuerMetadata](issuer-metadata.md) | [release]<br>val [issuerMetadata](issuer-metadata.md): CredentialIssuerMetadata<br>issuer metadata |
| [offeredDocuments](offered-documents.md) | [release]<br>val [offeredDocuments](offered-documents.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Offer.OfferedDocument](-offered-document/index.md)&gt;<br>offered documents |
| [txCodeSpec](tx-code-spec.md) | [release]<br>val [txCodeSpec](tx-code-spec.md): TxCode?<br>offered documents |