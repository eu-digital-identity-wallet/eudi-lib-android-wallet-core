//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.format](../index.md)/[MsoMdocClaim](index.md)

# MsoMdocClaim

[release]\
data class [MsoMdocClaim](index.md)(val nameSpace: [NameSpace](../../eu.europa.ec.eudi.wallet.document/-name-space/index.md), val identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val value: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?, val rawValue: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), val issuerMetadata: [IssuerMetadata.Claim](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/-claim/index.md)?) : [DocumentClaim](../-document-claim/index.md)

Represents a claim of a document in the MsoMdoc format.

## Constructors

| | |
|---|---|
| [MsoMdocClaim](-mso-mdoc-claim.md) | [release]<br>constructor(nameSpace: [NameSpace](../../eu.europa.ec.eudi.wallet.document/-name-space/index.md), identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), value: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?, rawValue: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), issuerMetadata: [IssuerMetadata.Claim](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/-claim/index.md)?) |

## Properties

| Name | Summary |
|---|---|
| [identifier](identifier.md) | [release]<br>open override val [identifier](identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The identifier of the claim. |
| [issuerMetadata](issuer-metadata.md) | [release]<br>open override val [issuerMetadata](issuer-metadata.md): [IssuerMetadata.Claim](../../eu.europa.ec.eudi.wallet.document.metadata/-issuer-metadata/-claim/index.md)?<br>The metadata of the claim provided by the issuer. |
| [nameSpace](name-space.md) | [release]<br>val [nameSpace](name-space.md): [NameSpace](../../eu.europa.ec.eudi.wallet.document/-name-space/index.md)<br>The name-space of the claim. |
| [rawValue](raw-value.md) | [release]<br>open override val [rawValue](raw-value.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)<br>The raw value of the claim in bytes. |
| [value](value.md) | [release]<br>open override val [value](value.md): [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?<br>The value of the claim. |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [release]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [release]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |