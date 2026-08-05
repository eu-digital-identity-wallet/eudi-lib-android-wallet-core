//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.metadata](../index.md)/[IssuerMetadata](index.md)

# IssuerMetadata

[release]\
@Serializable

data class [IssuerMetadata](index.md)(val documentConfigurationIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val display: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Display](-display/index.md)&gt;, val claims: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Claim](-claim/index.md)&gt;?, val credentialIssuerIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val issuerDisplay: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.IssuerDisplay](-issuer-display/index.md)&gt;?)

Document metadata domain object for storage.

## Constructors

| | |
|---|---|
| [IssuerMetadata](-issuer-metadata.md) | [release]<br>constructor(documentConfigurationIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), display: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Display](-display/index.md)&gt;, claims: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Claim](-claim/index.md)&gt;?, credentialIssuerIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), issuerDisplay: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.IssuerDisplay](-issuer-display/index.md)&gt;?) |

## Types

| Name | Summary |
|---|---|
| [Claim](-claim/index.md) | [release]<br>@Serializable<br>data class [Claim](-claim/index.md)(val path: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;, val mandatory: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)? = false, val display: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Claim.Display](-claim/-display/index.md)&gt; = emptyList())<br>Claim properties. |
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |
| [Display](-display/index.md) | [release]<br>@Serializable<br>data class [Display](-display/index.md)(val name: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val locale: [Locale](https://developer.android.com/reference/kotlin/java/util/Locale.html)? = null, val logo: [IssuerMetadata.Logo](-logo/index.md)? = null, val description: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null, val backgroundColor: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null, val textColor: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null, val backgroundImageUri: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)? = null)<br>Display properties of a supported credential type for a certain language. |
| [IssuerDisplay](-issuer-display/index.md) | [release]<br>@Serializable<br>data class [IssuerDisplay](-issuer-display/index.md)(val name: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val locale: [Locale](https://developer.android.com/reference/kotlin/java/util/Locale.html)? = null, val logo: [IssuerMetadata.Logo](-logo/index.md)? = null)<br>Display properties of the issuer that issued the document. |
| [Logo](-logo/index.md) | [release]<br>@Serializable<br>data class [Logo](-logo/index.md)(val uri: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)? = null, val alternativeText: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null)<br>Logo information. |

## Properties

| Name | Summary |
|---|---|
| [claims](claims.md) | [release]<br>val [claims](claims.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Claim](-claim/index.md)&gt;?<br>metadata for the claims of the document |
| [credentialIssuerIdentifier](credential-issuer-identifier.md) | [release]<br>val [credentialIssuerIdentifier](credential-issuer-identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the credential issuer identifier |
| [display](display.md) | [release]<br>val [display](display.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Display](-display/index.md)&gt;<br>the display properties of the document |
| [documentConfigurationIdentifier](document-configuration-identifier.md) | [release]<br>val [documentConfigurationIdentifier](document-configuration-identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the document configuration identifier |
| [issuerDisplay](issuer-display.md) | [release]<br>val [issuerDisplay](issuer-display.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.IssuerDisplay](-issuer-display/index.md)&gt;?<br>the display properties of the issuer that issued the document |

## Functions

| Name | Summary |
|---|---|
| [toJson](to-json.md) | [release]<br>fun [toJson](to-json.md)(): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>Convert the object to a JSON string. |