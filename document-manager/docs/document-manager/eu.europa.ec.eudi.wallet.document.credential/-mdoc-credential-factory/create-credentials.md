//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[MdocCredentialFactory](index.md)/[createCredentials](create-credentials.md)

# createCredentials

[release]\
open suspend override fun [createCredentials](create-credentials.md)(format: [DocumentFormat](../../eu.europa.ec.eudi.wallet.document.format/-document-format/index.md), document: Document, createDocumentSettings: [CreateDocumentSettings](../../eu.europa.ec.eudi.wallet.document/-create-document-settings/index.md), secureArea: SecureArea): [Pair](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-pair/index.html)&lt;[List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;MdocCredential&gt;, [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?&gt;

Creates mDL credentials for a document based on MSO mDOC format settings.

#### Return

a list of MdocCredential instances bound to the document

#### Parameters

release

| | |
|---|---|
| format | the document format, must be an instance of MsoMdocFormat |
| document | the document that will contain the credentials |
| createDocumentSettings | settings for creating the document credentials |
| secureArea | the secure area for storing cryptographic keys |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if the provided format is not an instance of MsoMdocFormat |