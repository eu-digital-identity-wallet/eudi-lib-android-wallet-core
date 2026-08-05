//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[CredentialFactory](index.md)

# CredentialFactory

fun interface [CredentialFactory](index.md)

Factory interface for creating credentials based on document format. Provides functionality to create appropriate credential types for different document formats.

#### Inheritors

| |
|---|
| [MdocCredentialFactory](../-mdoc-credential-factory/index.md) |
| [SdJwtVcCredentialFactory](../-sd-jwt-vc-credential-factory/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [createCredentials](create-credentials.md) | [release]<br>abstract suspend fun [createCredentials](create-credentials.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), document: Document, createDocumentSettings: [CreateDocumentSettings](../../eu.europa.ec.eudi.wallet.document/-create-document-settings/index.md), secureArea: SecureArea): [Pair](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-pair/index.html)&lt;[List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureAreaBoundCredential&gt;, [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?&gt;<br>Creates a list of credentials for a document based on the given format and settings. |