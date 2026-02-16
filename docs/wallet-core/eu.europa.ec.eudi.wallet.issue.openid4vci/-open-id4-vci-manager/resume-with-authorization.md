//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[resumeWithAuthorization](resume-with-authorization.md)

# resumeWithAuthorization

[androidJvm]\
abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))

Resume the authorization flow after the user has been redirected back to the app.

**Note:** This method should only be called when using the default [BrowserAuthorizationHandler](../-browser-authorization-handler/index.md). If you are using a custom [AuthorizationHandler](../-authorization-handler/index.md) implementation, you are responsible for providing the authorization code and state directly to your handler. Custom handlers should manage their own authorization flow completion without relying on this method.

#### Parameters

androidJvm

| | |
|---|---|
| uri | the uri that contains the authorization code and state parameters |

#### See also

| |
|---|
| [BrowserAuthorizationHandler](../-browser-authorization-handler/index.md) |
| [AuthorizationHandler](../-authorization-handler/index.md) |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | if no authorization request to resume or if a custom     [AuthorizationHandler](../-authorization-handler/index.md) is being used |

[androidJvm]\
abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Resume the authorization flow after the user has been redirected back to the app.

**Note:** This method should only be called when using the default [BrowserAuthorizationHandler](../-browser-authorization-handler/index.md). If you are using a custom [AuthorizationHandler](../-authorization-handler/index.md) implementation, you are responsible for providing the authorization code and state directly to your handler. Custom handlers should manage their own authorization flow completion without relying on this method.

#### Parameters

androidJvm

| | |
|---|---|
| uri | the uri string that contains the authorization code and state parameters |

#### See also

| |
|---|
| [BrowserAuthorizationHandler](../-browser-authorization-handler/index.md) |
| [AuthorizationHandler](../-authorization-handler/index.md) |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | if no authorization request to resume or if a custom     [AuthorizationHandler](../-authorization-handler/index.md) is being used |
