//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../../index.md)/[DPopConfig](../index.md)/[Default](index.md)

# Default

[androidJvm]\
data object [Default](index.md) : [DPopConfig](../index.md)

Default DPoP configuration using Android Keystore.

This configuration automatically sets up DPoP with sensible defaults:

- 
   Keys stored in Android Keystore
- 
   Storage in app's no-backup files directory
- 
   StrongBox support when available
- 
   No user authentication required for key usage
- 
   Algorithm automatically negotiated with the server

The actual configuration is created lazily when needed by calling make.

## Example

```kotlin
val openId4VciConfig = OpenId4VciManager.Config.Builder()
    .withIssuerUrl("https://issuer.com")
    .withClientId("client-id")
    .withAuthFlowRedirectionURI("app://callback")
    .withDPopConfig(DPopConfig.Default)
    .build()
```
