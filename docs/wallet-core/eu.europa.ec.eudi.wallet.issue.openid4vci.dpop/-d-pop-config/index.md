//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[DPopConfig](index.md)

# DPopConfig

sealed interface [DPopConfig](index.md)

Configuration for DPoP (Demonstrating Proof-of-Possession) in OpenID4VCI credential issuance.

DPoP is a security mechanism that binds OAuth 2.0 access tokens to cryptographic keys, preventing token theft and replay attacks. This sealed interface defines the available DPoP configuration options for credential issuance flows.

## Available Configurations

- 
   [Disabled](-disabled/index.md) - DPoP is not used
- 
   [Default](-default/index.md) - Uses Android Keystore with default settings
- 
   [Custom](-custom/index.md) - Uses a custom secure area with custom key settings

## Example Usage

```kotlin
// Disable DPoP
val config = DPopConfig.Disabled

// Use default configuration
val config = DPopConfig.Default

// Use custom secure area
val config = DPopConfig.Custom(
    secureArea = mySecureArea,
    createKeySettingsBuilder = { algorithms ->
        MyCreateKeySettings(algorithms.first())
    }
)
```

#### See also

| |
|---|
| [DPopConfig.Disabled](-disabled/index.md) |
| [DPopConfig.Default](-default/index.md) |
| [DPopConfig.Custom](-custom/index.md) |

#### Inheritors

| |
|---|
| [Disabled](-disabled/index.md) |
| [Default](-default/index.md) |
| [Custom](-custom/index.md) |

## Types

| Name | Summary |
|---|---|
| [Custom](-custom/index.md) | [androidJvm]<br>data class [Custom](-custom/index.md)(val secureArea: SecureArea, val createKeySettingsBuilder: ([List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) -&gt; CreateKeySettings, val keyUnlockDataProvider: [KeyUnlockDataProvider](../-key-unlock-data-provider/index.md) = KeyUnlockDataProvider.None) : [DPopConfig](index.md)<br>Custom DPoP configuration using a specific secure area. |
| [Default](-default/index.md) | [androidJvm]<br>data object [Default](-default/index.md) : [DPopConfig](index.md)<br>Default DPoP configuration using Android Keystore. |
| [Disabled](-disabled/index.md) | [androidJvm]<br>data object [Disabled](-disabled/index.md) : [DPopConfig](index.md)<br>DPoP is disabled. |
