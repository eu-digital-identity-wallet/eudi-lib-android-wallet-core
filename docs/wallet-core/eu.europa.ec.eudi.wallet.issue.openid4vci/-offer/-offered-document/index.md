//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[Offer](../index.md)/[OfferedDocument](index.md)

# OfferedDocument

[release]\
data class [OfferedDocument](index.md)(val offer: [Offer](../index.md), val configurationIdentifier: CredentialConfigurationIdentifier, val configuration: CredentialConfiguration)

Represents an offered document part of an [Offer](../index.md).

## Constructors

| | |
|---|---|
| [OfferedDocument](-offered-document.md) | [release]<br>constructor(offer: [Offer](../index.md), configurationIdentifier: CredentialConfigurationIdentifier, configuration: CredentialConfiguration) |

## Properties

| Name | Summary |
|---|---|
| [batchCredentialIssuanceSize](batch-credential-issuance-size.md) | [release]<br>val [batchCredentialIssuanceSize](batch-credential-issuance-size.md): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the batch credential issuance size based on the issuer metadata. If the issuer does not support batch credential issuance, returns 1. |
| [configuration](configuration.md) | [release]<br>val [configuration](configuration.md): CredentialConfiguration<br>credential configuration |
| [configurationIdentifier](configuration-identifier.md) | [release]<br>val [configurationIdentifier](configuration-identifier.md): CredentialConfigurationIdentifier<br>credential configuration identifier |
| [documentFormat](document-format.md) | [release]<br>val [documentFormat](document-format.md): DocumentFormat?<br>Returns the document format based on the credential configuration. |
| [offer](offer.md) | [release]<br>val [offer](offer.md): [Offer](../index.md)<br>[Offer](../index.md) instance |

## Functions

| Name | Summary |
|---|---|
| [extractIssuerMetadata](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.transformations/extract-issuer-metadata.md) | [release]<br>fun [Offer.OfferedDocument](index.md).[extractIssuerMetadata](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.transformations/extract-issuer-metadata.md)(): IssuerMetadata |