//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[SecureAreaWalletKeyManager](index.md)/[getWalletAttestationKey](get-wallet-attestation-key.md)

# getWalletAttestationKey

[androidJvm]\
open suspend override fun [getWalletAttestationKey](get-wallet-attestation-key.md)(keyAlias: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [WalletAttestationKey](../-wallet-attestation-key/index.md)?

Retrieves the existing Wallet Attestation Key for the specified Authorization Server URL. If no key exists for the given Authorization Server, it returns null.

#### Return

The existing [WalletAttestationKey](../-wallet-attestation-key/index.md) or null if not found.

#### Parameters

androidJvm

| | |
|---|---|
| keyAlias | The URL of the Authorization Server. |
