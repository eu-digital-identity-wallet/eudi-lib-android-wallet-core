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
| [clientId](client-id.md) | [androidJvm]<br>var [clientId](client-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>the client id |
| [dPoPUsage](d-po-p-usage.md) | [androidJvm]<br>var [dPoPUsage](d-po-p-usage.md): [OpenId4VciManager.Config.DPoPUsage](../-d-po-p-usage/index.md)<br>flag that if set will enable the use of DPoP JWT |
| [issuerUrl](issuer-url.md) | [androidJvm]<br>var [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>the issuer url |
| [parUsage](par-usage.md) | [androidJvm]<br>var [parUsage](par-usage.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>if PAR should be used |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [OpenId4VciManager.Config](../index.md)<br>Build the [Config](../index.md) |
| [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md) | [androidJvm]<br>fun [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md)(authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): &lt;Error class: unknown class&gt;<br>Set the redirection URI for the authorization flow |
| [withClientId](with-client-id.md) | [androidJvm]<br>fun [withClientId](with-client-id.md)(clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): &lt;Error class: unknown class&gt;<br>Set the client id |
| [withDPoPUsage](with-d-po-p-usage.md) | [androidJvm]<br>fun [withDPoPUsage](with-d-po-p-usage.md)(dPoPUsage: [OpenId4VciManager.Config.DPoPUsage](../-d-po-p-usage/index.md)): &lt;Error class: unknown class&gt;<br>Set the flag to enable the use of DPoP JWT |
| [withIssuerUrl](with-issuer-url.md) | [androidJvm]<br>fun [withIssuerUrl](with-issuer-url.md)(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): &lt;Error class: unknown class&gt;<br>Set the issuer url |
| [withParUsage](with-par-usage.md) | [androidJvm]<br>fun [withParUsage](with-par-usage.md)(parUsage: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): &lt;Error class: unknown class&gt;<br>Set the PAR usage |
