//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document.metadata](../../index.md)/[IssuerMetadata](../index.md)/[Claim](index.md)

# Claim

@Serializable

data class [Claim](index.md)(val path: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;, val mandatory: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)? = false, val display: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Claim.Display](-display/index.md)&gt; = emptyList())

Claim properties.

#### See also

| |
|---|
| [Display](-display/index.md) |

## Constructors

| | |
|---|---|
| [Claim](-claim.md) | [release]<br>constructor(path: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;, mandatory: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)? = false, display: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Claim.Display](-display/index.md)&gt; = emptyList()) |

## Types

| Name | Summary |
|---|---|
| [Display](-display/index.md) | [release]<br>@Serializable<br>data class [Display](-display/index.md)(val name: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null, val locale: [Locale](https://developer.android.com/reference/kotlin/java/util/Locale.html)? = null)<br>Display properties of a Claim. |

## Properties

| Name | Summary |
|---|---|
| [display](display.md) | [release]<br>@SerialName(value = &quot;display&quot;)<br>val [display](display.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[IssuerMetadata.Claim.Display](-display/index.md)&gt;<br>the display properties of the claim |
| [mandatory](mandatory.md) | [release]<br>@SerialName(value = &quot;mandatory&quot;)<br>val [mandatory](mandatory.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)?<br>whether the claim is mandatory |
| [path](path.md) | [release]<br>@SerialName(value = &quot;path&quot;)<br>val [path](path.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>namespace,element identifier in case of mso_mdoc, and claim path in case sd-jwt-vc |