//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[resumeOpenId4VciWithAuthorization](resume-open-id4-vci-with-authorization.md)

# resumeOpenId4VciWithAuthorization

[androidJvm]\
fun [resumeOpenId4VciWithAuthorization](resume-open-id4-vci-with-authorization.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))

Resumes the OpenId4VCI flow with the given [intent](resume-open-id4-vci-with-authorization.md)

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
fun [resumeOpenId4VciWithAuthorization](resume-open-id4-vci-with-authorization.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

fun [resumeOpenId4VciWithAuthorization](resume-open-id4-vci-with-authorization.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))

Resumes the OpenId4VCI flow with the given intent

#### Parameters

androidJvm

| | |
|---|---|
| uri | the uri that contains the authorization code |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if no authorization request to resume |
