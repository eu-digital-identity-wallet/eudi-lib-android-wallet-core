//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[SecureAreaDpopSigner](index.md)/[acquire](acquire.md)

# acquire

[androidJvm]\
open suspend override fun [acquire](acquire.md)(): SignOperation&lt;JWK&gt;

Acquires a signing operation for creating DPoP proofs.

This method prepares a SignOperation that can be used to sign DPoP proof JWTs. The operation includes:

1. 
   **Public Key Material**: The public key as a JWK (JSON Web Key) that will be     included in the DPoP proof header
2. 
   **Signing Function**: A function that signs input data using the private key     stored in the secure area

## Signing Process

When the signing function is invoked:

1. 
   The [DPopConfig.Custom.keyUnlockDataProvider](../-d-pop-config/-custom/key-unlock-data-provider.md) callback is called to obtain unlock data (if required)
2. 
   The secure area signs the input data using the private key
3. 
   The signature is returned in DER-encoded format

## User Authentication

If the key requires user authentication (configured via [DPopConfig.Custom.createKeySettingsBuilder](../-d-pop-config/-custom/create-key-settings-builder.md)), the [DPopConfig.Custom.keyUnlockDataProvider](../-d-pop-config/-custom/key-unlock-data-provider.md) function must prompt the user (e.g., via biometric prompt) and provide the unlock data before signing can proceed.

## Example

```kotlin
val signOperation = signer.acquire()
try {
    val publicKey = signOperation.publicMaterial // JWK for DPoP header
    val signature = signOperation.function(dataToSign) // Create signature
    // Use publicKey and signature in DPoP proof
} finally {
    signer.release(signOperation)
}
```

#### Return

A SignOperation containing the public JWK and signing function
