//[document-manager](../../../../../index.md)/[eu.europa.ec.eudi.wallet.document](../../../index.md)/[CreateDocumentSettings](../../index.md)/[CredentialPolicy](../index.md)/[RotateUse](index.md)

# RotateUse

data object [RotateUse](index.md) : [CreateDocumentSettings.CredentialPolicy](../index.md)

Policy that manages credential rotation by tracking usage count.

When a credential is used, its usage count is incremented, allowing the system to distribute load across multiple available credentials. This approach balances security with performance considerations by enabling credential reuse while maintaining usage patterns for auditing and optimization purposes.

Appropriate for scenarios requiring frequent authentication where the performance overhead of continuous credential generation would be prohibitive.

#### See also

| | |
|---|---|
| [OneTimeUse](../-one-time-use/index.md) | for a stricter security policy |