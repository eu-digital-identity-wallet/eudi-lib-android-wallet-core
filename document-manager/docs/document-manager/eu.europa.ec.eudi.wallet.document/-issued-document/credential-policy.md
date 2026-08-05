//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[credentialPolicy](credential-policy.md)

# credentialPolicy

[release]\
val [credentialPolicy](credential-policy.md): [CreateDocumentSettings.CredentialPolicy](../-create-document-settings/-credential-policy/index.md)

The credential policy associated with this document.

This property determines how credentials are managed after cryptographic operations:

- 
   [CreateDocumentSettings.CredentialPolicy.OneTimeUse](../-create-document-settings/-credential-policy/-one-time-use/index.md): The credential is deleted after use
- 
   [CreateDocumentSettings.CredentialPolicy.RotateUse](../-create-document-settings/-credential-policy/-rotate-use/index.md): The credential's usage count is incremented after use

#### See also

| |
|---|
| [CreateDocumentSettings.CredentialPolicy](../-create-document-settings/-credential-policy/index.md) |