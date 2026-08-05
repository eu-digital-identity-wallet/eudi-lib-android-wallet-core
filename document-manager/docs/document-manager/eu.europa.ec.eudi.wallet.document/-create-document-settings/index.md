//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[CreateDocumentSettings](index.md)

# CreateDocumentSettings

interface [CreateDocumentSettings](index.md)

Interface that defines the required creationSettings when creating a document with [DocumentManager.createDocument](../-document-manager/create-document.md). Implementors of [DocumentManager](../-document-manager/index.md) may introduce custom requirements for creating a document.

#### See also

| | |
|---|---|
| [CreateDocumentSettingsImpl](../-create-document-settings-impl/index.md) | implementation |

#### Inheritors

| |
|---|
| [CreateDocumentSettingsImpl](../-create-document-settings-impl/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |
| [CredentialPolicy](-credential-policy/index.md) | [release]<br>sealed interface [CredentialPolicy](-credential-policy/index.md) |

## Properties

| Name | Summary |
|---|---|
| [createKeySettings](create-key-settings.md) | [release]<br>abstract val [createKeySettings](create-key-settings.md): CreateKeySettings<br>Configuration settings for key creation within the secure area. These settings define properties such as key algorithms, key sizes, and any other parameters required by the underlying secure area implementation. |
| [credentialPolicy](credential-policy.md) | [release]<br>abstract val [credentialPolicy](credential-policy.md): [CreateDocumentSettings.CredentialPolicy](-credential-policy/index.md)<br>Defines the policy for credential usage and lifecycle management. Controls whether credentials are used once and deleted or rotated through multiple uses. |
| [numberOfCredentials](number-of-credentials.md) | [release]<br>abstract val [numberOfCredentials](number-of-credentials.md): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>Specifies the number of credentials to create for this document. Multiple credentials can be used for load balancing or redundancy purposes. Must be greater than 0. |
| [secureAreaIdentifier](secure-area-identifier.md) | [release]<br>abstract val [secureAreaIdentifier](secure-area-identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>Identifier for the secure area where document keys will be stored. This should match an existing secure area in the system. |