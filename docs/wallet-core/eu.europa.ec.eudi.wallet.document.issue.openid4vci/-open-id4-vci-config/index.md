//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document.issue.openid4vci](../index.md)/[OpenId4VciConfig](index.md)

# OpenId4VciConfig

[androidJvm]\
class [OpenId4VciConfig](index.md)

Configuration for the OpenID4VCI issuer.

Use [Builder](-builder/index.md) to create an instance.

Example usage:

```kotlin
val config = OpenId4VciConfig.Builder()
   .withIssuerUrl("https://example.issuer.com")
   .withClientId("client-id")
   .build()
```

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)<br>Builder for [OpenId4VciConfig](index.md). |

## Properties

| Name | Summary |
|---|---|
| [clientId](client-id.md) | [androidJvm]<br>val [clientId](client-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the client id |
| [issuerUrl](issuer-url.md) | [androidJvm]<br>val [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the issuer url |
