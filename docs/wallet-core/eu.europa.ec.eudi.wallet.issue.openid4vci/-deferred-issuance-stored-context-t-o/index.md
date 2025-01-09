//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DeferredIssuanceStoredContextTO](index.md)

# DeferredIssuanceStoredContextTO

[androidJvm]\
@Serializable

data class [DeferredIssuanceStoredContextTO](index.md)(val
credentialIssuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
val clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
clientAttestationJwt: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? =
null, val
clientAttestationPopDuration: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? =
null, val
clientAttestationPopAlgorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? =
null, val
clientAttestationPopType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? =
null, val
clientAttestationPopKeyId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? =
null, val
deferredEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
val authServerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
val tokenEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
val
dPoPSignerKid: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? =
null, val responseEncryptionSpec: JsonObject? = null, val
transactionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
accessToken: [AccessTokenTO](../-access-token-t-o/index.md), val
refreshToken: [RefreshTokenTO](../-refresh-token-t-o/index.md)? = null, val
authorizationTimestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html),
val grant: [GrantTO](../-grant-t-o/index.md))

## Constructors

|                                                                             |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DeferredIssuanceStoredContextTO](-deferred-issuance-stored-context-t-o.md) | [androidJvm]<br>constructor(credentialIssuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), clientAttestationJwt: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, clientAttestationPopDuration: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null, clientAttestationPopAlgorithm: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, clientAttestationPopType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, clientAttestationPopKeyId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, deferredEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), authServerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tokenEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), dPoPSignerKid: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, responseEncryptionSpec: JsonObject? = null, transactionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), accessToken: [AccessTokenTO](../-access-token-t-o/index.md), refreshToken: [RefreshTokenTO](../-refresh-token-t-o/index.md)? = null, authorizationTimestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), grant: [GrantTO](../-grant-t-o/index.md)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name                                                                 | Summary                                                                                                                                                                                                                                           |
|----------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [accessToken](access-token.md)                                       | [androidJvm]<br>@SerialName(value = &quot;access_token&quot;)<br>val [accessToken](access-token.md): [AccessTokenTO](../-access-token-t-o/index.md)                                                                                               |
| [authorizationTimestamp](authorization-timestamp.md)                 | [androidJvm]<br>@SerialName(value = &quot;authorization_timestamp&quot;)<br>val [authorizationTimestamp](authorization-timestamp.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)                                |
| [authServerId](auth-server-id.md)                                    | [androidJvm]<br>@Required<br>@SerialName(value = &quot;auth_server_id&quot;)<br>val [authServerId](auth-server-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                           |
| [clientAttestationJwt](client-attestation-jwt.md)                    | [androidJvm]<br>@SerialName(value = &quot;client_attestation_jwt&quot;)<br>val [clientAttestationJwt](client-attestation-jwt.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null                        |
| [clientAttestationPopAlgorithm](client-attestation-pop-algorithm.md) | [androidJvm]<br>@SerialName(value = &quot;client_attestation_pop_alg&quot;)<br>val [clientAttestationPopAlgorithm](client-attestation-pop-algorithm.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [clientAttestationPopDuration](client-attestation-pop-duration.md)   | [androidJvm]<br>@SerialName(value = &quot;client_attestation_pop_duration&quot;)<br>val [clientAttestationPopDuration](client-attestation-pop-duration.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)? = null  |
| [clientAttestationPopKeyId](client-attestation-pop-key-id.md)        | [androidJvm]<br>@SerialName(value = &quot;client_attestation_pop_key_id&quot;)<br>val [clientAttestationPopKeyId](client-attestation-pop-key-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null     |
| [clientAttestationPopType](client-attestation-pop-type.md)           | [androidJvm]<br>@SerialName(value = &quot;client_attestation_pop_typ&quot;)<br>val [clientAttestationPopType](client-attestation-pop-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null           |
| [clientId](client-id.md)                                             | [androidJvm]<br>@Required<br>@SerialName(value = &quot;client_id&quot;)<br>val [clientId](client-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                                         |
| [credentialIssuerId](credential-issuer-id.md)                        | [androidJvm]<br>@Required<br>@SerialName(value = &quot;credential_issuer&quot;)<br>val [credentialIssuerId](credential-issuer-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                            |
| [deferredEndpoint](deferred-endpoint.md)                             | [androidJvm]<br>@Required<br>@SerialName(value = &quot;deferred_endpoint&quot;)<br>val [deferredEndpoint](deferred-endpoint.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                 |
| [dPoPSignerKid](d-po-p-signer-kid.md)                                | [androidJvm]<br>@SerialName(value = &quot;dpop_key_id&quot;)<br>val [dPoPSignerKid](d-po-p-signer-kid.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null                                               |
| [grant](grant.md)                                                    | [androidJvm]<br>@SerialName(value = &quot;grant&quot;)<br>val [grant](grant.md): [GrantTO](../-grant-t-o/index.md)                                                                                                                                |
| [refreshToken](refresh-token.md)                                     | [androidJvm]<br>@SerialName(value = &quot;refresh_token&quot;)<br>val [refreshToken](refresh-token.md): [RefreshTokenTO](../-refresh-token-t-o/index.md)? = null                                                                                  |
| [responseEncryptionSpec](response-encryption-spec.md)                | [androidJvm]<br>@SerialName(value = &quot;credential_response_encryption_spec&quot;)<br>val [responseEncryptionSpec](response-encryption-spec.md): JsonObject? = null                                                                             |
| [tokenEndpoint](token-endpoint.md)                                   | [androidJvm]<br>@Required<br>@SerialName(value = &quot;token_endpoint&quot;)<br>val [tokenEndpoint](token-endpoint.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                          |
| [transactionId](transaction-id.md)                                   | [androidJvm]<br>@SerialName(value = &quot;transaction_id&quot;)<br>val [transactionId](transaction-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                                       |

## Functions

| Name | Summary |
|---|---|
| [toDeferredIssuanceStoredContext](to-deferred-issuance-stored-context.md) | [androidJvm]<br>fun [toDeferredIssuanceStoredContext](to-deferred-issuance-stored-context.md)(clock: [Clock](https://developer.android.com/reference/kotlin/java/time/Clock.html), recreatePopSigner: ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) -&gt; PopSigner.Jwt?, recreateClientAttestationPodSigner: ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) -&gt; JWSSigner?): DeferredIssuanceContext |
