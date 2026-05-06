//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)

# Builder

[release]\
class [Builder](index.md)

Builder for [Config](../index.md)

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md) | [release]<br>var [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?<br>the redirection URI for the authorization flow |
| [authorizationHandler](authorization-handler.md) | [release]<br>var [authorizationHandler](authorization-handler.md): [AuthorizationHandler](../../../-authorization-handler/index.md)?<br>the handler for authorization requests. If null, uses [BrowserAuthorizationHandler](../../../-browser-authorization-handler/index.md) |
| [clientAuthenticationType](client-authentication-type.md) | [release]<br>var [clientAuthenticationType](client-authentication-type.md): [OpenId4VciManager.ClientAuthenticationType](../../-client-authentication-type/index.md)? |
| [dpopConfig](dpop-config.md) | [release]<br>var [dpopConfig](dpop-config.md): [DPopConfig](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md)<br>The DPoP configuration for credential issuance. |
| [issuanceMetadataStorage](issuance-metadata-storage.md) | [release]<br>var [issuanceMetadataStorage](issuance-metadata-storage.md): Storage? |
| [issuerUrl](issuer-url.md) | [release]<br>var [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?<br>the issuer url |
| [parUsage](par-usage.md) | [release]<br>var [parUsage](par-usage.md): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)<br>if PAR should be used |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [OpenId4VciManager.Config](../index.md)<br>Build the [Config](../index.md) |
| [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md) | [release]<br>fun [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md)(authFlowRedirectionURI: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the redirection URI for the authorization flow |
| [withAuthorizationHandler](with-authorization-handler.md) | [release]<br>fun [withAuthorizationHandler](with-authorization-handler.md)(authorizationHandler: [AuthorizationHandler](../../../-authorization-handler/index.md)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the authorization handler for handling authorization requests. If not set, [BrowserAuthorizationHandler](../../../-browser-authorization-handler/index.md) will be used by default. |
| [withClientAuthenticationType](with-client-authentication-type.md) | [release]<br>fun [withClientAuthenticationType](with-client-authentication-type.md)(clientAuthenticationType: [OpenId4VciManager.ClientAuthenticationType](../../-client-authentication-type/index.md)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the client authentication type |
| [withDPopConfig](with-d-pop-config.md) | [release]<br>fun [withDPopConfig](with-d-pop-config.md)(dpopConfig: [DPopConfig](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md)): [OpenId4VciManager.Config.Builder](index.md)<br>Sets the DPoP (Demonstrating Proof-of-Possession) configuration. |
| [withIssuanceMetadataStorage](with-issuance-metadata-storage.md) | [release]<br>fun [withIssuanceMetadataStorage](with-issuance-metadata-storage.md)(storage: Storage?): [OpenId4VciManager.Config.Builder](index.md)<br>Sets the storage for issuance metadata. |
| [withIssuerUrl](with-issuer-url.md) | [release]<br>fun [withIssuerUrl](with-issuer-url.md)(issuerUrl: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the issuer url |
| [withParUsage](with-par-usage.md) | [release]<br>fun [withParUsage](with-par-usage.md)(parUsage: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the PAR usage |