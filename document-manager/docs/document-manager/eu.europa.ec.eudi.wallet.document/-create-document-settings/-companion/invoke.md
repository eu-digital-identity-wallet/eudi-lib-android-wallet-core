//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document](../../index.md)/[CreateDocumentSettings](../index.md)/[Companion](index.md)/[invoke](invoke.md)

# invoke

[release]\
operator fun [invoke](invoke.md)(secureAreaIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), createKeySettings: CreateKeySettings, numberOfCredentials: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) = 1, credentialPolicy: [CreateDocumentSettings.CredentialPolicy](../-credential-policy/index.md) = CredentialPolicy.RotateUse): [CreateDocumentSettings](../index.md)

Create a new instance of [CreateDocumentSettings](../index.md) for [DocumentManagerImpl.createDocument](../../-document-manager-impl/create-document.md) that uses the org.multipaz.securearea.SecureArea.

#### Return

A new instance of [CreateDocumentSettings](../index.md)

#### Parameters

release

| | |
|---|---|
| secureAreaIdentifier | The identifier from org.multipaz.securearea.SecureArea where the document's keys should be stored |
| createKeySettings | The CreateKeySettings implementation that accompanies the provided org.multipaz.securearea.SecureArea |
| numberOfCredentials | The number of credentials to create for this document. Must be greater than 0. Defaults to 1 if not specified. |
| credentialPolicy | The policy determining how credentials are managed after use. Defaults to [CredentialPolicy.RotateUse](../-credential-policy/-rotate-use/index.md) if not specified. |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if numberOfCredentials is not greater than 0 |