//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[AuthorizationResponse](index.md)

# AuthorizationResponse

[androidJvm]\
data class [AuthorizationResponse](index.md)(val authorizationCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val serverState: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Response from the authorization flow containing the authorization code and server state.

## Constructors

| | |
|---|---|
| [AuthorizationResponse](-authorization-response.md) | [androidJvm]<br>constructor(authorizationCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), serverState: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [authorizationCode](authorization-code.md) | [androidJvm]<br>val [authorizationCode](authorization-code.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authorization code received from the authorization server |
| [serverState](server-state.md) | [androidJvm]<br>val [serverState](server-state.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The state parameter from the authorization server used for CSRF protection |
