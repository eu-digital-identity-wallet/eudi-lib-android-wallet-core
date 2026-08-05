//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document](../../index.md)/[CreateDocumentSettings](../index.md)/[Companion](index.md)

# Companion

[release]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | [release]<br>operator fun [invoke](invoke.md)(secureAreaIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), createKeySettings: CreateKeySettings, numberOfCredentials: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) = 1, credentialPolicy: [CreateDocumentSettings.CredentialPolicy](../-credential-policy/index.md) = CredentialPolicy.RotateUse): [CreateDocumentSettings](../index.md)<br>Create a new instance of [CreateDocumentSettings](../index.md) for [DocumentManagerImpl.createDocument](../../-document-manager-impl/create-document.md) that uses the org.multipaz.securearea.SecureArea. |