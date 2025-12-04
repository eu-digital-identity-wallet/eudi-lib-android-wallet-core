//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.provider](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [DefaultWalletKeyManager](-default-wallet-key-manager/index.md) | [androidJvm]<br>class [DefaultWalletKeyManager](-default-wallet-key-manager/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) : [WalletKeyManager](-wallet-key-manager/index.md)<br>The default Android implementation of [WalletKeyManager](-wallet-key-manager/index.md). |
| [SecureAreaWalletKeyManager](-secure-area-wallet-key-manager/index.md) | [androidJvm]<br>open class [SecureAreaWalletKeyManager](-secure-area-wallet-key-manager/index.md)(secureArea: SecureArea, createKeySettingsProvider: suspend (Algorithm) -&gt; CreateKeySettings, keyUnlockDataProvider: suspend ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), SecureArea) -&gt; KeyUnlockData? = { _, _ -&gt; null }) : [WalletKeyManager](-wallet-key-manager/index.md)<br>A generic implementation of [WalletKeyManager](-wallet-key-manager/index.md) that delegates cryptographic operations to a provided SecureArea. |
| [WalletAttestationKey](-wallet-attestation-key/index.md) | [androidJvm]<br>open class [WalletAttestationKey](-wallet-attestation-key/index.md)(val keyInfo: KeyInfo, val signFunction: suspend ([ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)) -&gt; [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)) |
| [WalletAttestationsProvider](-wallet-attestations-provider/index.md) | [androidJvm]<br>interface [WalletAttestationsProvider](-wallet-attestations-provider/index.md)<br>Interface defining the bridge between the Wallet Core SDK and the Wallet Provider Service. |
| [WalletKeyManager](-wallet-key-manager/index.md) | [androidJvm]<br>interface [WalletKeyManager](-wallet-key-manager/index.md)<br>Manages the cryptographic keys used for Client Authentication and Attestation binding. |
