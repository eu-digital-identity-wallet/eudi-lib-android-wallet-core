//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[CredentialFactory](index.md)/[createCredentials](create-credentials.md)

# createCredentials

[release]\
abstract suspend fun [createCredentials](create-credentials.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), document: Document, createDocumentSettings: [CreateDocumentSettings](../../eu.europa.ec.eudi.wallet.document/-create-document-settings/index.md), secureArea: SecureArea): [Pair](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-pair/index.html)&lt;[List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureAreaBoundCredential&gt;, [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?&gt;

Creates a list of credentials for a document based on the given format and settings.

#### Return

a list of credentials bound to the secure area

#### Parameters

release

| | |
|---|---|
| format | the document format that determines the type of credential to create |
| document | the document that will contain the credentials |
| createDocumentSettings | settings for creating the document credentials |
| secureArea | the secure area for storing cryptographic keys |