//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)

Builder to create an instance of [Config](../index.md)

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor() |

## Functions

| Name                                                              | Summary                                                                                                                                                                                                                                                                                                                              |
|-------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md)          | [androidJvm]<br>fun [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md)(authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the redirection URI for the authorization flow                                           |
| [build](build.md)                                                 | [androidJvm]<br>fun [build](build.md)(): [OpenId4VciManager.Config](../index.md)<br>Build the [Config](../index.md)                                                                                                                                                                                                                  |
| [clientId](client-id.md)                                          | [androidJvm]<br>fun [clientId](client-id.md)(clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the client id                                                                                                                          |
| [issuerUrl](issuer-url.md)                                        | [androidJvm]<br>fun [issuerUrl](issuer-url.md)(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the issuer url                                                                                                                      |
| [useDPoP](use-d-po-p.md)                                          | [androidJvm]<br>fun [useDPoP](use-d-po-p.md)(useDPoP: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the flag that if set will enable the use of DPoP JWT                                                                                  |
| [useStrongBoxIfSupported](use-strong-box-if-supported.md)         | [androidJvm]<br>fun [useStrongBoxIfSupported](use-strong-box-if-supported.md)(useStrongBoxIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [OpenId4VciManager.Config.Builder](index.md)<br>Set the flag that if set will enable the use of StrongBox for document keys if supported |
| [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md) | [androidJvm]<br>fun [withAuthFlowRedirectionURI](with-auth-flow-redirection-u-r-i.md)(authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)                                                                                        |
| [withClientId](with-client-id.md)                                 | [androidJvm]<br>fun [withClientId](with-client-id.md)(clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)                                                                                                                                      |
| [withIssuerUrl](with-issuer-url.md)                               | [androidJvm]<br>fun [withIssuerUrl](with-issuer-url.md)(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VciManager.Config.Builder](index.md)                                                                                                                                   |

## Properties

| Name                                                      | Summary                                                                                                                                                                                                           |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md)  | [androidJvm]<br>var [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>the redirection URI for the authorization flow |
| [clientId](client-id.md)                                  | [androidJvm]<br>var [clientId](client-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>the client id                                                                  |
| [issuerUrl](issuer-url.md)                                | [androidJvm]<br>var [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>the issuer url                                                               |
| [useDPoP](use-d-po-p.md)                                  | [androidJvm]<br>var [useDPoP](use-d-po-p.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>flag that if set will enable the use of DPoP JWT                              |
| [useStrongBoxIfSupported](use-strong-box-if-supported.md) | [androidJvm]<br>var [useStrongBoxIfSupported](use-strong-box-if-supported.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>use StrongBox for document keys if supported |
