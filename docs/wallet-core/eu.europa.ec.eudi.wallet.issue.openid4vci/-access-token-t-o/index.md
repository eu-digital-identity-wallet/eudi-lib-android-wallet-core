//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[AccessTokenTO](index.md)

# AccessTokenTO

[androidJvm]\
@Serializable

data class [AccessTokenTO](index.md)(val type: [AccessTokenTypeTO](../-access-token-type-t-o/index.md), val accessToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val expiresIn: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null)

## Constructors

| | |
|---|---|
| [AccessTokenTO](-access-token-t-o.md) | [androidJvm]<br>constructor(type: [AccessTokenTypeTO](../-access-token-type-t-o/index.md), accessToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), expiresIn: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [accessToken](access-token.md) | [androidJvm]<br>@Required<br>@SerialName(value = &quot;access_token&quot;)<br>val [accessToken](access-token.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [expiresIn](expires-in.md) | [androidJvm]<br>@SerialName(value = &quot;expires_in&quot;)<br>val [expiresIn](expires-in.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null |
| [type](type.md) | [androidJvm]<br>@Required<br>@SerialName(value = &quot;type&quot;)<br>val [type](type.md): [AccessTokenTypeTO](../-access-token-type-t-o/index.md) |

## Functions

| Name | Summary |
|---|---|
| [toAccessToken](to-access-token.md) | [androidJvm]<br>fun [toAccessToken](to-access-token.md)(): AccessToken |
