//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[RefreshTokenTO](index.md)

# RefreshTokenTO

[androidJvm]\
@Serializable

data class [RefreshTokenTO](index.md)(val refreshToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val expiresIn: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null)

## Constructors

| | |
|---|---|
| [RefreshTokenTO](-refresh-token-t-o.md) | [androidJvm]<br>constructor(refreshToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), expiresIn: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [expiresIn](expires-in.md) | [androidJvm]<br>@SerialName(value = &quot;expires_in&quot;)<br>val [expiresIn](expires-in.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null |
| [refreshToken](refresh-token.md) | [androidJvm]<br>@Required<br>@SerialName(value = &quot;refresh_token&quot;)<br>val [refreshToken](refresh-token.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Functions

| Name | Summary |
|---|---|
| [toRefreshToken](to-refresh-token.md) | [androidJvm]<br>fun [toRefreshToken](to-refresh-token.md)(): RefreshToken |
