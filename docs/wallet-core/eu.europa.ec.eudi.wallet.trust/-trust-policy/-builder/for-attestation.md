//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../../index.md)/[TrustPolicy](../index.md)/[Builder](index.md)/[forAttestation](for-attestation.md)

# forAttestation

[androidJvm]\
fun [forAttestation](for-attestation.md)(identifier: AttestationIdentifier, action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;

Adds an override for a specific AttestationIdentifier.

This takes the highest priority in resolution order.

#### Return

this builder for chaining

#### Parameters

androidJvm

| | |
|---|---|
| identifier | the attestation identifier to match |
| action | the action to return when the identifier matches |
