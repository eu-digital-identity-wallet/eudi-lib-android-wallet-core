//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../../index.md)/[DPopConfig](../index.md)/[Custom](index.md)

# Custom

[androidJvm]\
data class [Custom](index.md)(val secureArea: SecureArea, val createKeySettingsBuilder: ([List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) -&gt; CreateKeySettings, val keyUnlockDataProvider: [KeyUnlockDataProvider](../../-key-unlock-data-provider/index.md) = KeyUnlockDataProvider.None) : [DPopConfig](../index.md)

Custom DPoP configuration using a specific secure area.

This configuration allows full control over how DPoP keys are created and stored. The algorithms for key creation are automatically selected based on what both the authorization server and the secure area support.

## How It Works

1. 
   The library checks the authorization server's supported DPoP algorithms
2. 
   It finds all algorithms supported by both server and [secureArea](secure-area.md)
3. 
   The list of matched algorithms is passed to [createKeySettingsBuilder](create-key-settings-builder.md)
4. 
   Your builder function selects an appropriate algorithm from the list
5. 
   A DPoP key is created using the returned settings
6. 
   The key is used to sign all DPoP proofs during issuance

## Example with Custom Secure Area

```kotlin
val customConfig = DPopConfig.Custom(
    secureArea = myCloudSecureArea,
    createKeySettingsBuilder = { algorithms ->
        val algorithm = algorithms.first() // Select the first supported algorithm
        CloudKeySettings.Builder()
            .setAlgorithm(algorithm)
            .setKeyName("dpop_key")
            .setRequireUserAuth(true)
            .build()
    }
)
```

## Example with Android Keystore

```kotlin
val androidConfig = DPopConfig.Custom(
    secureArea = AndroidKeystoreSecureArea.create(storage),
    createKeySettingsBuilder = { algorithms ->
        // Select the first signing algorithm from the list
        val algorithm = algorithms.firstOrNull { it.isSigning }
            ?: throw IllegalStateException("No signing algorithm available")
        AndroidKeystoreCreateKeySettings.Builder(attestationChallenge)
            .setAlgorithm(algorithm) // e.g., ES256, ES384, ES512
            .setUserAuthenticationRequired(true, 30_000, setOf(
                UserAuthenticationType.BIOMETRIC
            ))
            .setUseStrongBox(true)
            .build()
    }
)
```

## Constructors

| | |
|---|---|
| [Custom](-custom.md) | [androidJvm]<br>constructor(secureArea: SecureArea, createKeySettingsBuilder: ([List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) -&gt; CreateKeySettings, keyUnlockDataProvider: [KeyUnlockDataProvider](../../-key-unlock-data-provider/index.md) = KeyUnlockDataProvider.None) |

## Properties

| Name | Summary |
|---|---|
| [createKeySettingsBuilder](create-key-settings-builder.md) | [androidJvm]<br>val [createKeySettingsBuilder](create-key-settings-builder.md): ([List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;) -&gt; CreateKeySettings<br>A function that creates CreateKeySettings from a list of     supported algorithms. The algorithms parameter contains all algorithms that are     supported by both the authorization server and the secure area. Your implementation     should select an appropriate algorithm from this list and return valid settings for     creating a key with that algorithm. |
| [keyUnlockDataProvider](key-unlock-data-provider.md) | [androidJvm]<br>val [keyUnlockDataProvider](key-unlock-data-provider.md): [KeyUnlockDataProvider](../../-key-unlock-data-provider/index.md)<br>A [KeyUnlockDataProvider](../../-key-unlock-data-provider/index.md) that provides KeyUnlockData for unlocking     the DPoP key when performing signing operations. This is invoked each time a DPoP proof     needs to be signed. The provider receives the key alias and secure area as parameters     and should return the appropriate unlock data, or null if no unlock is required. |
| [secureArea](secure-area.md) | [androidJvm]<br>val [secureArea](secure-area.md): SecureArea<br>The secure area where DPoP keys are stored and managed.     Must support at least one signing algorithm (e.g., ES256, ES384, ES512). |
