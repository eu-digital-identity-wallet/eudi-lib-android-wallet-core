//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)

Builder for [Config](../index.md)

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md) | [androidJvm]<br>var [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>the redirection URI for the authorization flow |
| [authorizationHandler](authorization-handler.md) | [androidJvm]<br>var [authorizationHandler](authorization-handler.md): [AuthorizationHandler](../../../-authorization-handler/index.md)?<br>the handler for authorization requests. If null, uses [BrowserAuthorizationHandler](../../../-browser-authorization-handler/index.md) |
| [clientAuthenticationType](client-authentication-type.md) | [androidJvm]<br>var [clientAuthenticationType](client-authentication-type.md): [OpenId4VciManager.ClientAuthenticationType](../../-client-authentication-type/index.md)? |
| [dpopConfig](dpop-config.md) | [androidJvm]<br>var [dpopConfig](dpop-config.md): [DPopConfig](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md)<br>The DPoP configuration for credential issuance. |
| [issuanceMetadataStorage](issuance-metadata-storage.md) | [androidJvm]<br>var [issuanceMetadataStorage](issuance-metadata-storage.md): Storage? |
| [issuerUrl](issuer-url.md) | [androidJvm]<br>var [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>the issuer url |
| [parUsage](par-usage.md) | [androidJvm]<br>var [parUsage](par-usage.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>if PAR should be used |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [OpenId4VciManager.Config](../index.md)<br>Build the [Config](../index.md) |
| [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md) | [androidJvm]<br>fun [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md)(authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): &lt;Error class: unknown class&gt;<br>Set the redirection URI for the authorization flow |
| [withAuthorizationHandler](with-authorization-handler.md) | [androidJvm]<br>fun [withAuthorizationHandler](with-authorization-handler.md)(authorizationHandler: [AuthorizationHandler](../../../-authorization-handler/index.md)): &lt;Error class: unknown class&gt;<br>Set the authorization handler for handling authorization requests. If not set, [BrowserAuthorizationHandler](../../../-browser-authorization-handler/index.md) will be used by default. |
| [withClientAuthenticationType](with-client-authentication-type.md) | [androidJvm]<br>fun [withClientAuthenticationType](with-client-authentication-type.md)(clientAuthenticationType: [OpenId4VciManager.ClientAuthenticationType](../../-client-authentication-type/index.md)): &lt;Error class: unknown class&gt;<br>Set the client authentication type |
| [withDPopConfig](with-d-pop-config.md) | [androidJvm]<br>fun [withDPopConfig](with-d-pop-config.md)(dpopConfig: [DPopConfig](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.dpop/-d-pop-config/index.md)): &lt;Error class: unknown class&gt;<br>Sets the DPoP (Demonstrating Proof-of-Possession) configuration. |
| [withIssuanceMetadataStorage](with-issuance-metadata-storage.md) | [androidJvm]<br>fun [withIssuanceMetadataStorage](with-issuance-metadata-storage.md)(storage: Storage?): &lt;Error class: unknown class&gt;<br>Sets the storage for issuance metadata. |
| [withIssuerUrl](with-issuer-url.md) | [androidJvm]<br>fun [withIssuerUrl](with-issuer-url.md)(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): &lt;Error class: unknown class&gt;<br>Set the issuer url |
| [withParUsage](with-par-usage.md) | [androidJvm]<br>fun [withParUsage](with-par-usage.md)(parUsage: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): &lt;Error class: unknown class&gt;<br>Set the PAR usage |
