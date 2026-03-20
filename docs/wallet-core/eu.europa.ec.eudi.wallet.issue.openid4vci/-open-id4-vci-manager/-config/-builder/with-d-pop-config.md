//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)/[withDPopConfig](with-d-pop-config.md)

# withDPopConfig

[androidJvm]\
fun [withDPopConfig](with-d-pop-config.md)(dpopConfig: [DPopConfig](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md)): &lt;Error class: unknown class&gt;

Sets the DPoP (Demonstrating Proof-of-Possession) configuration.

DPoP is a security mechanism that cryptographically binds access tokens to keys, preventing token theft and replay attacks during credential issuance.

## How It Works

When DPoP is enabled:

1. 
   A cryptographic key pair is created in a secure area
2. 
   For each token request, a DPoP proof JWT is generated and signed
3. 
   The authorization server validates the proof and binds the token to the key
4. 
   The same key must be used for all subsequent requests with that token

## Configuration Options

[**DPopConfig.Default**](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/-default/index.md) - Recommended for most applications:

- 
   Uses Android Keystore for secure key storage
- 
   Keys backed by hardware security when available
- 
   Algorithm negotiated automatically with the server
- 
   No user authentication required

[**DPopConfig.Custom**](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/-custom/index.md) - For advanced use cases:

- 
   Custom secure area (e.g., cloud HSM, custom keystore)
- 
   Custom key creation settings per algorithm
- 
   Optional user authentication (biometric/PIN)
- 
   Full control over key lifecycle

[**DPopConfig.Disabled**](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/-disabled/index.md) - Not recommended for production:

- 
   No cryptographic binding of tokens
- 
   More vulnerable to token theft
- 
   Use only for testing or when server doesn't support DPoP

## Examples

**Default configuration (recommended):**

```kotlin
withDPopConfig(DPopConfig.Default)
```

**Custom configuration with user authentication:**

```kotlin
val customConfig = DPopConfig.Custom(
    secureArea = AndroidKeystoreSecureArea.create(storage),
    createKeySettingsBuilder = { algorithm ->
        AndroidKeystoreCreateKeySettings.Builder(challenge)
            .setAlgorithm(algorithm)
            .setUserAuthenticationRequired(true, 30_000, setOf(
                UserAuthenticationType.BIOMETRIC
            ))
            .setUseStrongBox(true)
            .build()
    },
    unlockKey = { keyAlias, secureArea ->
        // Prompt for biometric authentication
        biometricPrompt.authenticate()
        keyUnlockData
    }
)
withDPopConfig(customConfig)
```

**Disable DPoP:**

```kotlin
withDPopConfig(DPopConfig.Disabled)
```

#### Return

This builder instance for method chaining

#### Parameters

androidJvm

| | |
|---|---|
| dpopConfig | The DPoP configuration to use. Defaults to [DPopConfig.Default](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/-default/index.md)     if not specified. |

#### See also

| | |
|---|---|
| [DPopConfig](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md) | for detailed configuration options |
| [DPopConfig.Default](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/-default/index.md) | for default configuration details |
| [DPopConfig.Custom](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/-custom/index.md) | for custom configuration options |
