//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[PreregisteredVerifier](index.md)

# PreregisteredVerifier

[androidJvm]\
data class [PreregisteredVerifier](index.md)(var clientId: [ClientId](../-client-id/index.md), var legalName: [LegalName](../-legal-name/index.md), var verifierApi: [VerifierApi](../-verifier-api/index.md), var jwsAlgorithm: [JwsAlgorithm](../-jws-algorithm/index.md) = JwsAlgorithm.ES256, var jwkSetSource: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html) = URI(&quot;$verifierApi/wallet/public-keys.json&quot;))

Preregistered verifier for the [ClientIdScheme.Preregistered](../-client-id-scheme/-preregistered/index.md) client identifier scheme.

## Constructors

| | |
|---|---|
| [PreregisteredVerifier](-preregistered-verifier.md) | [androidJvm]<br>constructor(clientId: [ClientId](../-client-id/index.md), legalName: [LegalName](../-legal-name/index.md), verifierApi: [VerifierApi](../-verifier-api/index.md), jwsAlgorithm: [JwsAlgorithm](../-jws-algorithm/index.md) = JwsAlgorithm.ES256, jwkSetSource: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html) = URI(&quot;$verifierApi/wallet/public-keys.json&quot;)) |

## Properties

| Name | Summary |
|---|---|
| [clientId](client-id.md) | [androidJvm]<br>var [clientId](client-id.md): [ClientId](../-client-id/index.md)<br>the client identifier |
| [jwkSetSource](jwk-set-source.md) | [androidJvm]<br>var [jwkSetSource](jwk-set-source.md): [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)<br>the JWK set source. Default is the [verifierApi](verifier-api.md) with the path &quot;/wallet/public-keys.json&quot; |
| [jwsAlgorithm](jws-algorithm.md) | [androidJvm]<br>var [jwsAlgorithm](jws-algorithm.md): [JwsAlgorithm](../-jws-algorithm/index.md)<br>the JWS algorithm. Default is [JwsAlgorithm.ES256](../-jws-algorithm/-e-s256/index.md) |
| [legalName](legal-name.md) | [androidJvm]<br>var [legalName](legal-name.md): [LegalName](../-legal-name/index.md)<br>the legal name of the client |
| [verifierApi](verifier-api.md) | [androidJvm]<br>var [verifierApi](verifier-api.md): [VerifierApi](../-verifier-api/index.md)<br>the verifier API |
