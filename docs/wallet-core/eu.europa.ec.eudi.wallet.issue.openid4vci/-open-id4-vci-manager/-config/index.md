//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[Config](index.md)

# Config

[release]\
data class [Config](index.md)@[JvmOverloads](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val issuerUrl: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val clientAuthenticationType: [OpenId4VciManager.ClientAuthenticationType](../-client-authentication-type/index.md), val authFlowRedirectionURI: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val authorizationHandler: [AuthorizationHandler](../../-authorization-handler/index.md)? = null, val dpopConfig: [DPopConfig](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md) = DPopConfig.Default, val parUsage: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) = IF_SUPPORTED, val issuanceMetadataStorage: Storage? = null)

Configuration for the OpenId4Vci issuer

## Constructors

| | |
|---|---|
| [Config](-config.md) | [release]<br>@[JvmOverloads](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>constructor(issuerUrl: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), clientAuthenticationType: [OpenId4VciManager.ClientAuthenticationType](../-client-authentication-type/index.md), authFlowRedirectionURI: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), authorizationHandler: [AuthorizationHandler](../../-authorization-handler/index.md)? = null, dpopConfig: [DPopConfig](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md) = DPopConfig.Default, parUsage: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) = IF_SUPPORTED, issuanceMetadataStorage: Storage? = null) |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [release]<br>class [Builder](-builder/index.md)<br>Builder for [Config](index.md) |
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |
| [ParUsage](-par-usage/index.md) | [release]<br>annotation class [ParUsage](-par-usage/index.md)<br>PAR usage for the OpenId4Vci issuer |

## Properties

| Name | Summary |
|---|---|
| [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md) | [release]<br>val [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the redirection URI for the authorization flow |
| [authorizationHandler](authorization-handler.md) | [release]<br>val [authorizationHandler](authorization-handler.md): [AuthorizationHandler](../../-authorization-handler/index.md)?<br>the handler for authorization requests. If null, uses [BrowserAuthorizationHandler](../../-browser-authorization-handler/index.md) |
| [clientAuthenticationType](client-authentication-type.md) | [release]<br>val [clientAuthenticationType](client-authentication-type.md): [OpenId4VciManager.ClientAuthenticationType](../-client-authentication-type/index.md) |
| [dpopConfig](dpop-config.md) | [release]<br>val [dpopConfig](dpop-config.md): [DPopConfig](../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md)<br>The DPoP (Demonstrating Proof-of-Possession) configuration for credential issuance.            DPoP binds OAuth 2.0 access tokens to cryptographic keys to prevent token theft and replay attacks. |
| [issuanceMetadataStorage](issuance-metadata-storage.md) | [release]<br>val [issuanceMetadataStorage](issuance-metadata-storage.md): Storage? |
| [issuerUrl](issuer-url.md) | [release]<br>val [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>the issuer url |
| [parUsage](par-usage.md) | [release]<br>val [parUsage](par-usage.md): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>if PAR should be used |