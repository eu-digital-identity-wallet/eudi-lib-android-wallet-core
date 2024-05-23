//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DefaultOpenId4VciManager](index.md)/[resumeWithAuthorization](resume-with-authorization.md)

# resumeWithAuthorization

[androidJvm]\
open override fun [resumeWithAuthorization](resume-with-authorization.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))

Resume the authorization flow after the user has been redirected back to the app

#### Parameters

androidJvm

| | |
|---|---|
| intent | the intent that contains the authorization code |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if no authorization request to resume |

[androidJvm]\
open override fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

open override fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))

Resume the authorization flow after the user has been redirected back to the app

#### Parameters

androidJvm

| | |
|---|---|
| uri | the uri that contains the authorization code |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if no authorization request to resume |
