//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.format](../index.md)/[SdJwtVcClaim](index.md)

# SdJwtVcClaim

[release]\
data class [SdJwtVcClaim](index.md)(val identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val value: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?, val rawValue: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val issuerMetadata: [IssuerMetadata.Claim](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/-claim/index.md)?, val selectivelyDisclosable: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html), val children: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[SdJwtVcClaim](index.md)&gt;) : [DocumentClaim](../-document-claim/index.md)

Represents a claim of a document in the SdJwtVc format.

## Constructors

| | |
|---|---|
| [SdJwtVcClaim](-sd-jwt-vc-claim.md) | [release]<br>constructor(identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), value: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?, rawValue: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), issuerMetadata: [IssuerMetadata.Claim](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/-claim/index.md)?, selectivelyDisclosable: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html), children: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[SdJwtVcClaim](index.md)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [children](children.md) | [release]<br>val [children](children.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[SdJwtVcClaim](index.md)&gt;<br>The children of the claim. |
| [identifier](identifier.md) | [release]<br>open override val [identifier](identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The identifier of the claim. |
| [issuerMetadata](issuer-metadata.md) | [release]<br>open override val [issuerMetadata](issuer-metadata.md): [IssuerMetadata.Claim](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/-claim/index.md)?<br>The metadata of the claim provided by the issuer. |
| [rawValue](raw-value.md) | [release]<br>open override val [rawValue](raw-value.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The raw value of the claim. |
| [selectivelyDisclosable](selectively-disclosable.md) | [release]<br>val [selectivelyDisclosable](selectively-disclosable.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>Whether the claim is selectively disclosable. |
| [value](value.md) | [release]<br>open override val [value](value.md): [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?<br>The value of the claim. |