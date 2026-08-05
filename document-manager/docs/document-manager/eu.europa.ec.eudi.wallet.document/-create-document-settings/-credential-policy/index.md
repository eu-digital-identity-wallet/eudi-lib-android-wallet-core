//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document](../../index.md)/[CreateDocumentSettings](../index.md)/[CredentialPolicy](index.md)

# CredentialPolicy

sealed interface [CredentialPolicy](index.md)

#### Inheritors

| |
|---|
| [OneTimeUse](-one-time-use/index.md) |
| [RotateUse](-rotate-use/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |
| [OneTimeUse](-one-time-use/index.md) | [release]<br>data object [OneTimeUse](-one-time-use/index.md) : [CreateDocumentSettings.CredentialPolicy](index.md)<br>Policy that deletes the credential after a single use. |
| [RotateUse](-rotate-use/index.md) | [release]<br>data object [RotateUse](-rotate-use/index.md) : [CreateDocumentSettings.CredentialPolicy](index.md)<br>Policy that manages credential rotation by tracking usage count. |