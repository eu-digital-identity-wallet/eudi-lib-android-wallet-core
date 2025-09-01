//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[Format](index.md)

# Format

sealed interface [Format](index.md)

Sealed interface defining the supported credential formats for OpenID4VP presentations.

This interface encapsulates the different types of verifiable credentials that can be presented through the OpenID4VP protocol. Each format defines specific requirements and capabilities for credential presentation and verification.

## Supported Formats:

- 
   [**SdJwtVc**](-sd-jwt-vc/index.md): Selective Disclosure JWT Verifiable Credentials with configurable algorithms
- 
   [**MsoMdoc**](-mso-mdoc/index.md): Mobile Security Object documents (ISO 18013-5 mDL format)

#### Since

1.0.0

#### See also

| |
|---|
| [OpenId4VpConfig.formats](../-open-id4-vp-config/formats.md) |

#### Inheritors

| |
|---|
| [SdJwtVc](-sd-jwt-vc/index.md) |
| [MsoMdoc](-mso-mdoc/index.md) |

## Types

| Name | Summary |
|---|---|
| [MsoMdoc](-mso-mdoc/index.md) | [androidJvm]<br>data class [MsoMdoc](-mso-mdoc/index.md)(val issuerAuthAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, val deviceAuthAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) : [Format](index.md)<br>Mobile Security Object document format (ISO 18013-5 mDL). |
| [SdJwtVc](-sd-jwt-vc/index.md) | [androidJvm]<br>data class [SdJwtVc](-sd-jwt-vc/index.md)(val sdJwtAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, val kbJwtAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) : [Format](index.md)<br>Selective Disclosure JWT Verifiable Credential format configuration. |
