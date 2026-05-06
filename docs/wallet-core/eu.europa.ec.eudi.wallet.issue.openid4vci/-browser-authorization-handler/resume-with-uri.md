//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[BrowserAuthorizationHandler](index.md)/[resumeWithUri](resume-with-uri.md)

# resumeWithUri

[release]\
fun [resumeWithUri](resume-with-uri.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))

Resumes the authorization from the given [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html). This should be called when the app receives the authorization callback via deep link.

This method extracts the authorization code and state from the callback URI and completes the suspended authorization coroutine with the result.

The suspended [authorize](authorize.md) coroutine will receive:

- 
   A successful [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) containing [AuthorizationResponse](../-authorization-response/index.md) if both 'code' and 'state' parameters are present
- 
   A failed [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) with [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) if the authorization code parameter ('code') is missing from the URI
- 
   A failed [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) with [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) if the server state parameter ('state') is missing from the URI

#### Parameters

release

| | |
|---|---|
| uri | The callback URI containing the authorization code and state parameters |

#### See also

| |
|---|
| [authorize](authorize.md) |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-state-exception/index.html) | if no authorization is in progress |