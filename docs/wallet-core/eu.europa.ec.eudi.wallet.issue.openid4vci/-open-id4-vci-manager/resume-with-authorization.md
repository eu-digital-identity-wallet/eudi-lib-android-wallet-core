//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[resumeWithAuthorization](resume-with-authorization.md)

# resumeWithAuthorization

[androidJvm]\
abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))

abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Resume the authorization flow after the user has been redirected back to the app

#### Parameters

androidJvm

| | |
|---|---|
| uri | the uri that contains the authorization code |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | if no authorization request to resume |
