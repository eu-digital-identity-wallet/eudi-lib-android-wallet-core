//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[SdJwtVcCredentialFactory](index.md)

# SdJwtVcCredentialFactory

[release]\
class [SdJwtVcCredentialFactory](index.md)(val domain: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)) : [CredentialFactory](../-credential-factory/index.md)

Implementation of CredentialFactory for creating SD-JWT VC (Selective Disclosure JWT Verifiable Credentials) according to RFC 9901.

## Constructors

| | |
|---|---|
| [SdJwtVcCredentialFactory](-sd-jwt-vc-credential-factory.md) | [release]<br>constructor(domain: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [domain](domain.md) | [release]<br>val [domain](domain.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the domain for the credentials |

## Functions

| Name | Summary |
|---|---|
| [createCredentials](create-credentials.md) | [release]<br>open suspend override fun [createCredentials](create-credentials.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), document: Document, createDocumentSettings: [CreateDocumentSettings](../../eu.europa.ec.eudi.wallet.document/-create-document-settings/index.md), secureArea: SecureArea): [Pair](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-pair/index.html)&lt;[List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;KeyBoundSdJwtVcCredential&gt;, [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?&gt;<br>Creates SD-JWT VC credentials for a document based on SD-JWT VC format settings. |