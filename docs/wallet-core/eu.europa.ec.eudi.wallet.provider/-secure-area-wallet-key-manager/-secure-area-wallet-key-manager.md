//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[SecureAreaWalletKeyManager](index.md)/[SecureAreaWalletKeyManager](-secure-area-wallet-key-manager.md)

# SecureAreaWalletKeyManager

[androidJvm]\
constructor(secureArea: SecureArea, createKeySettingsProvider: suspend (Algorithm) -&gt; CreateKeySettings, keyUnlockDataProvider: suspend ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), SecureArea) -&gt; KeyUnlockData? = { _, _ -&gt; null })

#### Parameters

androidJvm

| | |
|---|---|
| secureArea | The underlying secure storage abstraction. |
| createKeySettingsProvider | A lambda that provides configuration for creating new keys given a selected Algorithm. |
| keyUnlockDataProvider | Optional provider for user-authentication data (e.g., Biometrics/PIN) if the key requires unlocking before use. Defaults to null. |
