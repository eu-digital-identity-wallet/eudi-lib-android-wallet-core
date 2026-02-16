//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[AuthorizationHandler](index.md)

# AuthorizationHandler

fun interface [AuthorizationHandler](index.md)

Handler for authorization requests during the OpenID4VCI flow.

Implementations of this interface are responsible for presenting the authorization URL to the user and obtaining the authorization response (code and state).

The default implementation [BrowserAuthorizationHandler](../-browser-authorization-handler/index.md) opens a browser for user authorization. Custom implementations can provide alternative authorization flows (e.g., in-app WebView, custom UI, embedded browsers).

#### Inheritors

| |
|---|
| [BrowserAuthorizationHandler](../-browser-authorization-handler/index.md) |

## Functions

| Name | Summary |
|---|---|
| [authorize](authorize.md) | [androidJvm]<br>abstract suspend fun [authorize](authorize.md)(authorizationUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[AuthorizationResponse](../-authorization-response/index.md)&gt;<br>Handles the authorization request by presenting the authorization URL to the user and eventually returning the authorization response. |
