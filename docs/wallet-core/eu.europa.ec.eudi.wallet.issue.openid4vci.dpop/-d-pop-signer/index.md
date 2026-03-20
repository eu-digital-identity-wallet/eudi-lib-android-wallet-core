//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[DPopSigner](index.md)

# DPopSigner

interface [DPopSigner](index.md) : Signer&lt;JWK&gt; 

Signer interface for DPoP (Demonstrating Proof-of-Possession) in OpenID4VCI flows.

DPoP is a security mechanism that binds OAuth 2.0 access tokens to cryptographic keys, preventing token theft and replay attacks. This interface extends the Signer interface with JWK (JSON Web Key) as the public key material type.

## How DPoP Works

1. 
   A cryptographic key pair is created in a secure area
2. 
   For each token request, a DPoP proof JWT is generated and signed with the private key
3. 
   The proof includes the public key (JWK), request details, and a timestamp
4. 
   The authorization server validates the proof and binds the access token to the public key
5. 
   The same key must be used for all subsequent requests with that token

## Creating a DPoP Signer

Use the eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopSigner.Companion.makeIfSupported factory method to create a DPoP signer:

```kotlin
val result = DPopSigner.makeIfSupported(
    context = context,
    config = DPopConfig.Default,
    authorizationServerMetadata = serverMetadata,
    logger = logger
)

result.onSuccess { signer ->
    // Use the signer for DPoP operations
}.onFailure { error ->
    // Handle error (e.g., DPoP not supported)
}
```

#### See also

| |
|---|
| Signer |
| JWK |
| [DPopConfig](../-d-pop-config/index.md) |
| DPopSigner.Companion.makeIfSupported |

#### Inheritors

| |
|---|
| [SecureAreaDpopSigner](../-secure-area-dpop-signer/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [javaAlgorithm](index.md#-1475265234%2FProperties%2F1615067946) | [androidJvm]<br>abstract val [javaAlgorithm](index.md#-1475265234%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |

## Functions

| Name | Summary |
|---|---|
| [acquire](index.md#890547045%2FFunctions%2F1615067946) | [androidJvm]<br>abstract suspend fun [acquire](index.md#890547045%2FFunctions%2F1615067946)(): SignOperation&lt;JWK&gt; |
| [release](index.md#-1674462898%2FFunctions%2F1615067946) | [androidJvm]<br>abstract suspend fun [release](index.md#-1674462898%2FFunctions%2F1615067946)(signOperation: SignOperation&lt;JWK&gt;?) |
