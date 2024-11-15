//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)

Builder for [Config](../index.md)

## Constructors

|                        |                               |
|------------------------|-------------------------------|
| [Builder](-builder.md) | [androidJvm]<br>constructor() |

## Properties

| Name                                                     | Summary                                                                                                                                                                                                           |
|----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md) | [androidJvm]<br>var [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>the redirection URI for the authorization flow |
| [clientId](client-id.md)                                 | [androidJvm]<br>var [clientId](client-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>the client id                                                                  |
| [issuerUrl](issuer-url.md)                               | [androidJvm]<br>var [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>the issuer url                                                               |
| [parUsage](par-usage.md)                                 | [androidJvm]<br>var [parUsage](par-usage.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>if PAR should be used                                                                 |
| [useDPoPIfSupported](use-d-po-p-if-supported.md)         | [androidJvm]<br>var [useDPoPIfSupported](use-d-po-p-if-supported.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>flag that if set will enable the use of DPoP JWT      |

## Functions

| Name                                                              | Summary                                                                                                                                                                                                                                                                                             |
|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [build](build.md)                                                 | [androidJvm]<br>fun [build](build.md)(): [OpenId4VciManager.Config](../index.md)<br>Build the [Config](../index.md)                                                                                                                                                                                 |
| [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md) | [androidJvm]<br>fun [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md)(authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the redirection URI for the authorization flow |
| [withClientId](with-client-id.md)                                 | [androidJvm]<br>fun [withClientId](with-client-id.md)(clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the client id                                                                                |
| [withIssuerUrl](with-issuer-url.md)                               | [androidJvm]<br>fun [withIssuerUrl](with-issuer-url.md)(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the issuer url                                                                            |
| [withParUsage](with-par-usage.md)                                 | [androidJvm]<br>fun [withParUsage](with-par-usage.md)(parUsage: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the PAR usage                                                                                      |
| [withUseDPoPIfSupported](with-use-d-po-p-if-supported.md)         | [androidJvm]<br>fun [withUseDPoPIfSupported](with-use-d-po-p-if-supported.md)(useDPoPIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the flag to enable the use of DPoP JWT                   |
