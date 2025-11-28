//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[WalletKeyManager](index.md)

# WalletKeyManager

fun interface [WalletKeyManager](index.md)

Manages the cryptographic keys used for Client Authentication and Attestation binding.

Responsible for creating, storing, and retrieving the cryptographic keys that those attestations certify.

#### Inheritors

| |
|---|
| [DefaultWalletKeyManager](../-default-wallet-key-manager/index.md) |
| [SecureAreaWalletKeyManager](../-secure-area-wallet-key-manager/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [getWalletAttestationKey](get-wallet-attestation-key.md) | [androidJvm]<br>abstract suspend fun [getWalletAttestationKey](get-wallet-attestation-key.md)(authorizationServerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), supportedAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[WalletAttestationKey](../-wallet-attestation-key/index.md)&gt;<br>Retrieves or creates a signing key to be used for Wallet Attestation (Client Authentication). The Wallet Attestation Keys must be distinct for different Authorization Servers but unique for a specific one, and should be stored for subsequent use with the same Authorization Server |
