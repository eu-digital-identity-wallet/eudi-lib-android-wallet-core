//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[DefaultWalletKeyManager](index.md)

# DefaultWalletKeyManager

class [DefaultWalletKeyManager](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) : [WalletKeyManager](../-wallet-key-manager/index.md)

The default Android implementation of [WalletKeyManager](../-wallet-key-manager/index.md).

This class handles the initialization of an AndroidKeystoreSecureArea backed by a dedicated file storage in the application's `noBackupFilesDir`.

#### Parameters

androidJvm

| | |
|---|---|
| context | The Android Application Context. |

## Constructors

| | |
|---|---|
| [DefaultWalletKeyManager](-default-wallet-key-manager.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) |

## Functions

| Name | Summary |
|---|---|
| [getOrCreateWalletAttestationKey](get-or-create-wallet-attestation-key.md) | [androidJvm]<br>open suspend override fun [getOrCreateWalletAttestationKey](get-or-create-wallet-attestation-key.md)(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), supportedAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[WalletAttestationKey](../-wallet-attestation-key/index.md)&gt;<br>Retrieves or creates a signing key to be used for Wallet Attestation (Client Authentication). The implementation must ensure that keys are scoped to the specific Authorization Server to prevent cross-service tracking (Unlinkability). The key alias is derived from the [issuerUrl](get-or-create-wallet-attestation-key.md). |
| [getWalletAttestationKey](get-wallet-attestation-key.md) | [androidJvm]<br>open suspend override fun [getWalletAttestationKey](get-wallet-attestation-key.md)(keyAlias: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [WalletAttestationKey](../-wallet-attestation-key/index.md)?<br>Retrieves the existing Wallet Attestation Key for the specified Authorization Server URL. If no key exists for the given Authorization Server, it returns null. |
