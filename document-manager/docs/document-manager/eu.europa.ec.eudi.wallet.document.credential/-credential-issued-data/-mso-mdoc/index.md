//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../../index.md)/[CredentialIssuedData](../index.md)/[MsoMdoc](index.md)

# MsoMdoc

[release]\
data class [MsoMdoc](index.md)(val nameSpacedData: NameSpacedData, val staticAuthData: StaticAuthDataParser.StaticAuthData) : [CredentialIssuedData](../index.md)

Represents Mobile Security Object (MSO) data for Mobile Driving License (mDL) credential format.

## Constructors

| | |
|---|---|
| [MsoMdoc](-mso-mdoc.md) | [release]<br>constructor(nameSpacedData: NameSpacedData, staticAuthData: StaticAuthDataParser.StaticAuthData) |

## Properties

| Name | Summary |
|---|---|
| [nameSpacedData](name-spaced-data.md) | [release]<br>val [nameSpacedData](name-spaced-data.md): NameSpacedData<br>The structured data containing namespace-organized credential attributes |
| [staticAuthData](static-auth-data.md) | [release]<br>val [staticAuthData](static-auth-data.md): StaticAuthDataParser.StaticAuthData<br>Authentication data provided by the issuer for verification purposes |