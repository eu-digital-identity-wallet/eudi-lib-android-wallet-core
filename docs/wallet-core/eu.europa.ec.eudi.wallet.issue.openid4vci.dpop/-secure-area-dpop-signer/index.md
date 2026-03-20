//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[SecureAreaDpopSigner](index.md)

# SecureAreaDpopSigner

class [SecureAreaDpopSigner](index.md) : [DPopSigner](../-d-pop-signer/index.md)

Secure area-based implementation of [DPopSigner](../-d-pop-signer/index.md) for OpenID4VCI credential issuance.

This class creates and manages a DPoP key in a secure area (such as Android Keystore) and uses it to sign DPoP proof JWTs. The key is created once during initialization and persists in the secure area for the duration of the credential issuance flow.

## Key Features

- 
   **Secure Key Storage**: Keys are stored in a hardware-backed secure area when available
- 
   **Algorithm Negotiation**: Uses an algorithm supported by both the server and secure area
- 
   **User Authentication**: Optionally requires user authentication (biometric/PIN) for signing
- 
   **Persistent Keys**: Keys remain in the secure area across multiple signing operations

## Lifecycle

1. 
   **Initialization**: A new key is created in the secure area with the specified algorithm
2. 
   **Signing**: The [acquire](acquire.md) method provides a signing function for creating DPoP proofs
3. 
   **Cleanup**: The [release](release.md) method is called but performs no action (keys persist)

## Usage

This class is typically created by DPopSigner.makeIfSupported and should not be instantiated directly by application code.

```kotlin
// With default configuration (recommended)
val signer = DPopSigner.makeIfSupported(
    context = context,
    config = DPopConfig.Default,
    authorizationServerMetadata = metadata
).getOrThrow()

// With custom secure area
val signer = DPopSigner.makeIfSupported(
    context = context,
    config = DPopConfig.Custom(
        secureArea = mySecureArea,
        createKeySettingsBuilder = { algorithms ->
            val algorithm = algorithms.firstOrNull { it.isSigning }
                ?: throw IllegalStateException("No signing algorithm available")
            MyKeySettings.Builder()
                .setAlgorithm(algorithm)
                .build()
        }
    ),
    authorizationServerMetadata = metadata
).getOrThrow()

// Use the signer
val signOperation = signer.acquire()
val signature = signOperation.function(dataToSign)
signer.release(signOperation)
```

#### Parameters

androidJvm

| | |
|---|---|
| algorithms | The list of cryptographic algorithms supported by both the authorization     server and the secure area (e.g., ES256, ES384, ES512). This list is passed to the     configuration's [DPopConfig.Custom.createKeySettingsBuilder](../-d-pop-config/-custom/create-key-settings-builder.md) to create the key with     an appropriate algorithm. The list is determined during DPopSigner.makeIfSupported     based on compatibility between the server and secure area. |
| logger | Optional logger for debugging and tracking DPoP key creation and signing operations. |

#### See also

| |
|---|
| [DPopSigner](../-d-pop-signer/index.md) |
| [DPopConfig.Custom](../-d-pop-config/-custom/index.md) |
| DPopSigner.Companion.makeIfSupported |

## Constructors

| | |
|---|---|
| [SecureAreaDpopSigner](-secure-area-dpop-signer.md) | [androidJvm]<br>constructor(config: [DPopConfig.Custom](../-d-pop-config/-custom/index.md), algorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null)<br>Creates a new DPoP signer with a fresh key in the specified secure area. |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [androidJvm]<br>val [config](config.md): [DPopConfig.Custom](../-d-pop-config/-custom/index.md)<br>The DPoP configuration containing the secure area, key settings builder,     and key unlock data provider. This configuration determines where keys are stored,     how they are protected, and how they are unlocked for signing operations. |
| [javaAlgorithm](java-algorithm.md) | [androidJvm]<br>open override val [javaAlgorithm](java-algorithm.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The Java algorithm identifier for the signing algorithm. |
| [keyInfo](key-info.md) | [androidJvm]<br>val [keyInfo](key-info.md): KeyInfo |

## Functions

| Name | Summary |
|---|---|
| [acquire](acquire.md) | [androidJvm]<br>open suspend override fun [acquire](acquire.md)(): SignOperation&lt;JWK&gt;<br>Acquires a signing operation for creating DPoP proofs. |
| [release](release.md) | [androidJvm]<br>open suspend override fun [release](release.md)(signOperation: SignOperation&lt;JWK&gt;?)<br>Releases resources associated with a signing operation. |
