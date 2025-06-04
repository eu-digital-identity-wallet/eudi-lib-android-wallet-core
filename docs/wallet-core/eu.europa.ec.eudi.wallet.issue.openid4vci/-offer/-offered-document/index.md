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
| [credentialPolicy](credential-policy.md) | [androidJvm]<br>val [credentialPolicy](credential-policy.md): CreateDocumentSettings.CredentialPolicy<br>Returns the credential policy based on the issuer metadata. |
| [documentFormat](document-format.md) | [androidJvm]<br>val [documentFormat](document-format.md): DocumentFormat?<br>Returns the document format based on the credential configuration. |
| [numberOfCredentials](number-of-credentials.md) | [androidJvm]<br>val [numberOfCredentials](number-of-credentials.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the number of credentials based on the issuer metadata. |
| [offer](offer.md) | [androidJvm]<br>val [offer](offer.md): [Offer](../index.md)<br>[Offer](../index.md) instance |

## Functions

| Name | Summary |
|---|---|
| [extractIssuerMetadata](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.transformations/extract-issuer-metadata.md) | [androidJvm]<br>fun [Offer.OfferedDocument](index.md).[extractIssuerMetadata](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.transformations/extract-issuer-metadata.md)(): IssuerMetadata |
