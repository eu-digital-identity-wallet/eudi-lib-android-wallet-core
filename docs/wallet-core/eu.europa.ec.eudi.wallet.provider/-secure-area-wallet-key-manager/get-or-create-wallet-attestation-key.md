//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[SecureAreaWalletKeyManager](index.md)/[getOrCreateWalletAttestationKey](get-or-create-wallet-attestation-key.md)

# getOrCreateWalletAttestationKey

[androidJvm]\
open suspend override fun [getOrCreateWalletAttestationKey](get-or-create-wallet-attestation-key.md)(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), supportedAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[WalletAttestationKey](../-wallet-attestation-key/index.md)&gt;

Retrieves or creates a signing key to be used for Wallet Attestation (Client Authentication). The implementation must ensure that keys are scoped to the specific Authorization Server to prevent cross-service tracking (Unlinkability). The key alias is derived from the [issuerUrl](get-or-create-wallet-attestation-key.md).

The implementation should ensure that the returned key is compatible with one of the [supportedAlgorithms](get-or-create-wallet-attestation-key.md) required by the Authorization Server.

#### Return

A [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) containing the [WalletAttestationKey](../-wallet-attestation-key/index.md), which includes the public key info and a mechanism to sign data.

#### Parameters

androidJvm

| | |
|---|---|
| issuerUrl | The Issuer Identifier of the Authorization Server This string is hashed to generate a unique, stable alias for the key in the Secure Area. |
| supportedAlgorithms | A list of cryptographic algorithms supported by the Authorization Server. The returned key must use one of these algorithms. |
