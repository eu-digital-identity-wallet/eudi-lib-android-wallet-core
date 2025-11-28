//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[WalletAttestationsProvider](index.md)/[getKeyAttestation](get-key-attestation.md)

# getKeyAttestation

[androidJvm]\
abstract suspend fun [getKeyAttestation](get-key-attestation.md)(keys: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;KeyInfo&gt;, nonce: Nonce?): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;

Retrieves the Wallet Unit Attestation (WUA) or Key Attestation.

This method is used when issuing with Attestation Proof Type or JWT with Attestation Proof Type

#### Return

A [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) containing the WUA as a signed JWT

#### Parameters

androidJvm

| | |
|---|---|
| keys | The list of public keys that need to be certified. These keys will be bound to the issuance session. |
| nonce | An optional nonce provided by the Issuer. If provided, it must be embedded in the attestation. |
