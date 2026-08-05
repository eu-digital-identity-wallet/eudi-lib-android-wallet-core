//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[CredentialIssuedData](index.md)

# CredentialIssuedData

sealed interface [CredentialIssuedData](index.md)

Sealed interface representing different types of credential data provided by issuers. This interface acts as a common type for various credential formats used within the EUDI Wallet.

#### Inheritors

| |
|---|
| [MsoMdoc](-mso-mdoc/index.md) |
| [SdJwtVc](-sd-jwt-vc/index.md) |

## Types

| Name | Summary |
|---|---|
| [MsoMdoc](-mso-mdoc/index.md) | [release]<br>data class [MsoMdoc](-mso-mdoc/index.md)(val nameSpacedData: NameSpacedData, val staticAuthData: StaticAuthDataParser.StaticAuthData) : [CredentialIssuedData](index.md)<br>Represents Mobile Security Object (MSO) data for Mobile Driving License (mDL) credential format. |
| [SdJwtVc](-sd-jwt-vc/index.md) | [release]<br>data class [SdJwtVc](-sd-jwt-vc/index.md)(val issuedSdJwt: SdJwt&lt;JwtAndClaims&gt;) : [CredentialIssuedData](index.md)<br>Represents a Selective Disclosure JWT Verifiable Credential format. |