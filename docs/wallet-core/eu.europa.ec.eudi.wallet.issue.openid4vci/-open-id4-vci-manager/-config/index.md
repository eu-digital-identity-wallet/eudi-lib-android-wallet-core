//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[Config](index.md)

# Config

[androidJvm]\
data class [Config](index.md)(val
issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
useStrongBoxIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), val
useDPoPIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), val
parUsage: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))

Configuration for the OpenId4Vci issuer

## Constructors

|                      |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Config](-config.md) | [androidJvm]<br>constructor(issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), useStrongBoxIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), useDPoPIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), parUsage: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |

## Types

| Name                             | Summary                                                                                                   |
|----------------------------------|-----------------------------------------------------------------------------------------------------------|
| [Builder](-builder/index.md)     | [androidJvm]<br>class [Builder](-builder/index.md)<br>Builder to create an instance of [Config](index.md) |
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md)                                                   |
| [ParUsage](-par-usage/index.md)  | [androidJvm]<br>annotation class [ParUsage](-par-usage/index.md)                                          |

## Properties

| Name                                                      | Summary                                                                                                                                                                                                           |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md)  | [androidJvm]<br>val [authFlowRedirectionURI](auth-flow-redirection-u-r-i.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the redirection URI for the authorization flow  |
| [clientId](client-id.md)                                  | [androidJvm]<br>val [clientId](client-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the client id                                                                   |
| [issuerUrl](issuer-url.md)                                | [androidJvm]<br>val [issuerUrl](issuer-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the issuer url                                                                |
| [parUsage](par-usage.md)                                  | [androidJvm]<br>val [parUsage](par-usage.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>if PAR should be used                                                                 |
| [useDPoPIfSupported](use-d-po-p-if-supported.md)          | [androidJvm]<br>val [useDPoPIfSupported](use-d-po-p-if-supported.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)                                                          |
| [useStrongBoxIfSupported](use-strong-box-if-supported.md) | [androidJvm]<br>val [useStrongBoxIfSupported](use-strong-box-if-supported.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>use StrongBox for document keys if supported |
