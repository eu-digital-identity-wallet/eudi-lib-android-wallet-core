//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[BrowserAuthorizationHandler](index.md)

# BrowserAuthorizationHandler

[androidJvm]\
class [BrowserAuthorizationHandler](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) : [AuthorizationHandler](../-authorization-handler/index.md)

Default implementation of [AuthorizationHandler](../-authorization-handler/index.md) that opens a browser for user authorization.

This handler:

1. 
   Opens the authorization URL in the system browser
2. 
   Waits for the app to receive the authorization callback via deep link
3. 
   Extracts the authorization code and state from the callback URI

## Constructors

| | |
|---|---|
| [BrowserAuthorizationHandler](-browser-authorization-handler.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) |

## Functions

| Name | Summary |
|---|---|
| [authorize](authorize.md) | [androidJvm]<br>open suspend override fun [authorize](authorize.md)(authorizationUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[AuthorizationResponse](../-authorization-response/index.md)&gt;<br>Handles the authorization request by presenting the authorization URL to the user and eventually returning the authorization response. |
| [cancel](cancel.md) | [androidJvm]<br>fun [cancel](cancel.md)()<br>Cancels any ongoing authorization request. |
| [resumeWithUri](resume-with-uri.md) | [androidJvm]<br>fun [resumeWithUri](resume-with-uri.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))<br>Resumes the authorization from the given [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html). This should be called when the app receives the authorization callback via deep link. |
