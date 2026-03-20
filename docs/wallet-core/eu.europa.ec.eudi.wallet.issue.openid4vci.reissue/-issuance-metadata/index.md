//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.reissue](../index.md)/[IssuanceMetadata](index.md)

# IssuanceMetadata

[androidJvm]\
@Serializable

data class [IssuanceMetadata](index.md)(val credentialIssuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val credentialConfigurationIdentifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val credentialEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val tokenEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val authorizationServerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val challengeEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val clientAttestationJwt: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val clientAttestationPopKeyId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val popKeyAliases: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val dPoPKeyAlias: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val accessToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val accessTokenType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val refreshToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val tokenTimestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val grantType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Configuration data required for credential re-issuance.

This data class stores all necessary information to re-issue a previously issued credential without requiring the user to go through the full authorization flow again. It uses the stored refresh token to obtain a new access token and issues a fresh credential.

Note: Encryption configuration (requestEncryptionSpec, responseEncryptionParams) is NOT stored because re-issuance creates a fresh Issuer via IssuerCreator, which automatically configures encryption support from the issuer's current metadata.

The data is persisted after successful credential issuance and can be used later to refresh the credential (e.g., when it expires or needs to be updated).

## Constructors

| | |
|---|---|
| [IssuanceMetadata](-issuance-metadata.md) | [androidJvm]<br>constructor(credentialIssuerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), credentialConfigurationIdentifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), credentialEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), tokenEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), authorizationServerId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), challengeEndpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), clientAttestationJwt: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, clientAttestationPopKeyId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, popKeyAliases: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, dPoPKeyAlias: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, accessToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), accessTokenType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), refreshToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, tokenTimestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), grantType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [accessToken](access-token.md) | [androidJvm]<br>val [accessToken](access-token.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The OAuth access token (may be expired) |
| [accessTokenType](access-token-type.md) | [androidJvm]<br>val [accessTokenType](access-token-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>&quot;Bearer&quot; or &quot;DPoP&quot; |
| [authorizationServerId](authorization-server-id.md) | [androidJvm]<br>val [authorizationServerId](authorization-server-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authorization server identifier (URL) |
| [challengeEndpoint](challenge-endpoint.md) | [androidJvm]<br>val [challengeEndpoint](challenge-endpoint.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>Optional challenge endpoint for attestation-based client authentication |
| [clientAttestationJwt](client-attestation-jwt.md) | [androidJvm]<br>val [clientAttestationJwt](client-attestation-jwt.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>Optional JWT for attestation-based client authentication (WIA) |
| [clientAttestationPopKeyId](client-attestation-pop-key-id.md) | [androidJvm]<br>val [clientAttestationPopKeyId](client-attestation-pop-key-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>Optional key ID for client attestation PoP signing |
| [clientId](client-id.md) | [androidJvm]<br>val [clientId](client-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The OAuth client ID |
| [credentialConfigurationIdentifier](credential-configuration-identifier.md) | [androidJvm]<br>val [credentialConfigurationIdentifier](credential-configuration-identifier.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The specific credential configuration ID that was issued |
| [credentialEndpoint](credential-endpoint.md) | [androidJvm]<br>val [credentialEndpoint](credential-endpoint.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The issuer's credential endpoint URL |
| [credentialIssuerId](credential-issuer-id.md) | [androidJvm]<br>val [credentialIssuerId](credential-issuer-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The credential issuer identifier (URL) |
| [dPoPKeyAlias](d-po-p-key-alias.md) | [androidJvm]<br>val [dPoPKeyAlias](d-po-p-key-alias.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>Optional DPoP key alias if DPoP was used in the original issuance |
| [grantType](grant-type.md) | [androidJvm]<br>val [grantType](grant-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>&quot;authorization_code&quot; or &quot;pre-authorized_code&quot; |
| [popKeyAliases](pop-key-aliases.md) | [androidJvm]<br>val [popKeyAliases](pop-key-aliases.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>List of PoP (Proof-of-Possession) key aliases used in the original issuance |
| [refreshToken](refresh-token.md) | [androidJvm]<br>val [refreshToken](refresh-token.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>Optional refresh token to obtain new access tokens |
| [tokenEndpoint](token-endpoint.md) | [androidJvm]<br>val [tokenEndpoint](token-endpoint.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The token endpoint URL for refreshing access tokens |
| [tokenTimestamp](token-timestamp.md) | [androidJvm]<br>val [tokenTimestamp](token-timestamp.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>Epoch seconds when the token was issued |

## Functions

| Name | Summary |
|---|---|
| [toByteArray](to-byte-array.md) | [androidJvm]<br>fun [toByteArray](to-byte-array.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)<br>Serializes this [IssuanceMetadata](index.md) to a [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html) for storage. |
