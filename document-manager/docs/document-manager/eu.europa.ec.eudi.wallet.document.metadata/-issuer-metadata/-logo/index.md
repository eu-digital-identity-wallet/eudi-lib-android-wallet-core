//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document.metadata](../../index.md)/[IssuerMetadata](../index.md)/[Logo](index.md)

# Logo

@Serializable

data class [Logo](index.md)(val uri: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)? = null, val alternativeText: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null)

Logo information.

#### See also

| |
|---|
| [URI](https://developer.android.com/reference/kotlin/java/net/URI.html) |

## Constructors

| | |
|---|---|
| [Logo](-logo.md) | [release]<br>constructor(uri: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)? = null, alternativeText: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null) |

## Properties

| Name | Summary |
|---|---|
| [alternativeText](alternative-text.md) | [release]<br>val [alternativeText](alternative-text.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?<br>the alternative text of the logo |
| [uri](uri.md) | [release]<br>@Serializable(with = URISerializer::class)<br>val [uri](uri.md): [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)?<br>the URI of the logo |