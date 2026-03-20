//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[KeyUnlockDataProvider](index.md)

# KeyUnlockDataProvider

fun interface [KeyUnlockDataProvider](index.md)

Functional interface for providing unlock data for DPoP keys stored in a secure area.

This interface is used to obtain KeyUnlockData when a DPoP key needs to be accessed for signing operations. Implementations can provide user authentication (e.g., biometric or PIN) when required by the key's security settings.

## Usage

The provider is invoked each time a DPoP proof JWT needs to be signed. If the key requires user authentication, the implementation should:

1. 
   Prompt the user for authentication (e.g., show biometric prompt)
2. 
   Wait for successful authentication
3. 
   Return the unlock data obtained from the authentication result
4. 
   Return null if no authentication is required

## Predefined Providers

- 
   [None](-companion/-none.md) - Returns null, suitable for keys that don't require authentication

## Examples

```kotlin
// No authentication required
val provider = KeyUnlockDataProvider.None

// Custom authentication with biometric prompt
val provider = KeyUnlockDataProvider { keyAlias, secureArea ->
    withContext(Dispatchers.Main) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate for DPoP")
            .setSubtitle("Sign credential request")
            .setNegativeButtonText("Cancel")
            .build()

        val result = showBiometricPrompt(promptInfo)
        result?.let { AndroidKeystoreKeyUnlockData(it.cryptoObject) }
    }
}

// Use in configuration
val config = DPopConfig.Custom(
    secureArea = secureArea,
    createKeySettingsBuilder = { algorithms -> /* ... */},
    keyUnlockDataProvider = provider
)
```

#### See also

| |
|---|
| KeyUnlockData |
| [DPopConfig.Custom](../-d-pop-config/-custom/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | [androidJvm]<br>abstract suspend operator fun [invoke](invoke.md)(keyAlias: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), secureArea: SecureArea): KeyUnlockData? |
