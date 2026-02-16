//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[BrowserAuthorizationHandler](index.md)/[resumeWithUri](resume-with-uri.md)

# resumeWithUri

[androidJvm]\
fun [resumeWithUri](resume-with-uri.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))

Resumes the authorization from the given [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html). This should be called when the app receives the authorization callback via deep link.

This method extracts the authorization code and state from the callback URI and completes the suspended authorization coroutine with the result.

The suspended [authorize](authorize.md) coroutine will receive:

- 
   A successful [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) containing [AuthorizationResponse](../-authorization-response/index.md) if both 'code' and 'state' parameters are present
- 
   A failed [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) with [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) if the authorization code parameter ('code') is missing from the URI
- 
   A failed [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) with [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) if the server state parameter ('state') is missing from the URI

#### Parameters

androidJvm

| | |
|---|---|
| uri | The callback URI containing the authorization code and state parameters |

#### See also

| |
|---|
| [BrowserAuthorizationHandler.authorize](authorize.md) |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | if no authorization is in progress |
