//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[CreateDocumentSettingsImpl](index.md)

# CreateDocumentSettingsImpl

data class [CreateDocumentSettingsImpl](index.md)(val secureAreaIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val createKeySettings: CreateKeySettings, val numberOfCredentials: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) = 1, val credentialPolicy: [CreateDocumentSettings.CredentialPolicy](../-create-document-settings/-credential-policy/index.md) = CredentialPolicy.RotateUse) : [CreateDocumentSettings](../-create-document-settings/index.md)

Implementation of [CreateDocumentSettings](../-create-document-settings/index.md) interface that provides configuration for document creation.

This class encapsulates all necessary parameters required when creating digital documents through the [DocumentManager.createDocument](../-document-manager/create-document.md) method. It specifies where document keys should be stored, how they should be created, and defines credential management policies.

#### See also

| | |
|---|---|
| [CreateDocumentSettings](../-create-document-settings/index.md) | The interface this class implements |
| [DocumentManager](../-document-manager/index.md) | For usage in document creation operations |
| [CredentialPolicy](../-create-document-settings/-credential-policy/index.md) | For available credential management policies |

## Constructors

| | |
|---|---|
| [CreateDocumentSettingsImpl](-create-document-settings-impl.md) | [release]<br>constructor(secureAreaIdentifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), createKeySettings: CreateKeySettings, numberOfCredentials: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) = 1, credentialPolicy: [CreateDocumentSettings.CredentialPolicy](../-create-document-settings/-credential-policy/index.md) = CredentialPolicy.RotateUse) |

## Properties

| Name | Summary |
|---|---|
| [createKeySettings](create-key-settings.md) | [release]<br>open override val [createKeySettings](create-key-settings.md): CreateKeySettings<br>The configuration settings for key creation within the specified secure area.                             These settings control properties such as key algorithms, sizes, and other                             security parameters required by the secure area implementation. |
| [credentialPolicy](credential-policy.md) | [release]<br>open override val [credentialPolicy](credential-policy.md): [CreateDocumentSettings.CredentialPolicy](../-create-document-settings/-credential-policy/index.md)<br>Defines how credentials are managed after use. Controls whether credentials are                            used once and deleted or rotated through multiple uses.                            Defaults to [CredentialPolicy.RotateUse](../-create-document-settings/-credential-policy/-rotate-use/index.md). |
| [numberOfCredentials](number-of-credentials.md) | [release]<br>open override val [numberOfCredentials](number-of-credentials.md): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>The number of credentials to create for this document. Multiple credentials                               can be used for load balancing or redundancy. Defaults to 1 if not specified.                               Must be greater than 0. |
| [secureAreaIdentifier](secure-area-identifier.md) | [release]<br>open override val [secureAreaIdentifier](secure-area-identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The secure area identifier where the document's keys should be stored.                                This identifier must reference an existing secure area in the system. |