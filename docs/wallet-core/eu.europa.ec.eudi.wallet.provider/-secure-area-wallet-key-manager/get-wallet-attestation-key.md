//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[SecureAreaWalletKeyManager](index.md)/[getWalletAttestationKey](get-wallet-attestation-key.md)

# getWalletAttestationKey

[androidJvm]\
open suspend override fun [getWalletAttestationKey](get-wallet-attestation-key.md)(authorizationServerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), supportedAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[WalletAttestationKey](../-wallet-attestation-key/index.md)&gt;

Retrieves or creates a signing key to be used for Wallet Attestation (Client Authentication). The Wallet Attestation Keys must be distinct for different Authorization Servers but unique for a specific one, and should be stored for subsequent use with the same Authorization Server

The implementation should ensure that the returned key is compatible with one of the [supportedAlgorithms](get-wallet-attestation-key.md) required by the Authorization Server.

#### Return

A [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) containing the [WalletAttestationKey](../-wallet-attestation-key/index.md), which includes the public key info and a mechanism to sign data.

#### Parameters

androidJvm

| | |
|---|---|
| authorizationServerUrl | The URL of the Authorization Server. |
| supportedAlgorithms | A list of cryptographic algorithms supported by the Authorization Server. The returned key must use one of these algorithms. |
