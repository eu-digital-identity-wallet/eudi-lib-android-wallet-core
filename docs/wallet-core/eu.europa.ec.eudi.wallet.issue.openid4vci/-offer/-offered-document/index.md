//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[Offer](../index.md)/[OfferedDocument](index.md)

# OfferedDocument

[androidJvm]\
data class [OfferedDocument](index.md)(val offer: [Offer](../index.md), val configurationIdentifier: CredentialConfigurationIdentifier, val configuration: CredentialConfiguration)

Represents an offered document part of an [Offer](../index.md).

## Constructors

| | |
|---|---|
| [OfferedDocument](-offered-document.md) | [androidJvm]<br>constructor(offer: [Offer](../index.md), configurationIdentifier: CredentialConfigurationIdentifier, configuration: CredentialConfiguration) |

## Properties

| Name | Summary |
|---|---|
| [configuration](configuration.md) | [androidJvm]<br>val [configuration](configuration.md): CredentialConfiguration<br>credential configuration |
| [configurationIdentifier](configuration-identifier.md) | [androidJvm]<br>val [configurationIdentifier](configuration-identifier.md): CredentialConfigurationIdentifier<br>credential configuration identifier |
| [documentFormat](document-format.md) | [androidJvm]<br>val [documentFormat](document-format.md): DocumentFormat? |
| [offer](offer.md) | [androidJvm]<br>val [offer](offer.md): [Offer](../index.md)<br>[Offer](../index.md) instance |

## Functions

| Name | Summary |
|---|---|
| [extractDocumentMetaData](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.transformations/extract-document-meta-data.md) | [androidJvm]<br>fun [Offer.OfferedDocument](index.md).[extractDocumentMetaData](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.transformations/extract-document-meta-data.md)(): DocumentMetaData |
