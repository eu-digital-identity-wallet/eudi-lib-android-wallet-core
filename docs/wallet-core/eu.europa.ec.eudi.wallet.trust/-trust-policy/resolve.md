//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[TrustPolicy](index.md)/[resolve](resolve.md)

# resolve

[androidJvm]\
abstract fun [resolve](resolve.md)(attestationIdentifier: AttestationIdentifier, verificationContext: VerificationContext?): [TrustPolicy.Action](-action/index.md)

Resolves the trust action for a given attestation identifier and verification context.

#### Return

the [Action](-action/index.md) indicating how the wallet should handle the trust verification result

#### Parameters

androidJvm

| | |
|---|---|
| attestationIdentifier | the type of credential being issued (e.g., MDoc or SDJwtVc) |
| verificationContext | the optional verification context (e.g., PID, QEAA), or null if unknown |
