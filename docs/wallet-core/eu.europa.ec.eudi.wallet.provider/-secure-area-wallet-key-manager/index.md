//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[SecureAreaWalletKeyManager](index.md)

# SecureAreaWalletKeyManager

open class [SecureAreaWalletKeyManager](index.md)(secureArea: SecureArea, createKeySettingsProvider: suspend (Algorithm) -&gt; CreateKeySettings, keyUnlockDataProvider: suspend ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), SecureArea) -&gt; KeyUnlockData? = { _, _ -&gt; null }) : [WalletKeyManager](../-wallet-key-manager/index.md)

A generic implementation of [WalletKeyManager](../-wallet-key-manager/index.md) that delegates cryptographic operations to a provided SecureArea.

Derives a stable key alias from the authorizationServerUrl (using SHA-256). Checks if a key exists in the SecureArea for that alias. If it exists and matches a supported algorithm, it is reused. If it does not exist or the algorithm is incompatible, a new key is generated.

#### Parameters

androidJvm

| | |
|---|---|
| secureArea | The underlying secure storage abstraction. |
| createKeySettingsProvider | A lambda that provides configuration for creating new keys given a selected Algorithm. |
| keyUnlockDataProvider | Optional provider for user-authentication data (e.g., Biometrics/PIN) if the key requires unlocking before use. Defaults to null. |

## Constructors

| | |
|---|---|
| [SecureAreaWalletKeyManager](-secure-area-wallet-key-manager.md) | [androidJvm]<br>constructor(secureArea: SecureArea, createKeySettingsProvider: suspend (Algorithm) -&gt; CreateKeySettings, keyUnlockDataProvider: suspend ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), SecureArea) -&gt; KeyUnlockData? = { _, _ -&gt; null }) |

## Functions

| Name | Summary |
|---|---|
| [getOrCreateWalletAttestationKey](get-or-create-wallet-attestation-key.md) | [androidJvm]<br>open suspend override fun [getOrCreateWalletAttestationKey](get-or-create-wallet-attestation-key.md)(authorizationServerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), supportedAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[WalletAttestationKey](../-wallet-attestation-key/index.md)&gt;<br>Retrieves or creates a signing key to be used for Wallet Attestation (Client Authentication). The Wallet Attestation Keys must be distinct for different Authorization Servers but unique for a specific one, and should be stored for subsequent use with the same Authorization Server |
| [getWalletAttestationKey](get-wallet-attestation-key.md) | [androidJvm]<br>open suspend override fun [getWalletAttestationKey](get-wallet-attestation-key.md)(keyAlias: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [WalletAttestationKey](../-wallet-attestation-key/index.md)?<br>Retrieves the existing Wallet Attestation Key for the specified Authorization Server URL. If no key exists for the given Authorization Server, it returns null. |
