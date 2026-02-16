//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[AuthorizationHandler](index.md)/[authorize](authorize.md)

# authorize

[androidJvm]\
abstract suspend fun [authorize](authorize.md)(authorizationUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[AuthorizationResponse](../-authorization-response/index.md)&gt;

Handles the authorization request by presenting the authorization URL to the user and eventually returning the authorization response.

This is a suspending function that should:

1. 
   Present the authorization URL to the user (e.g., open a browser)
2. 
   Wait for the user to complete authorization
3. 
   Return the authorization code and server state

#### Return

Result containing the [AuthorizationResponse](../-authorization-response/index.md) with authorization code and server state,     or a failure if authorization fails or is cancelled

#### Parameters

androidJvm

| | |
|---|---|
| authorizationUrl | The URL to present to the user for authorization |
