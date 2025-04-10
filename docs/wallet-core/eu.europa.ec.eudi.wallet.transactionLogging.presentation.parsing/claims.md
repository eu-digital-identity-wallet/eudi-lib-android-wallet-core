//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[claims](claims.md)

# claims

[androidJvm]\
val SdJwt&lt;JwtAndClaims&gt;.[claims](claims.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, JsonElement?&gt;

This extension property is used to get the claims from the sd-jwt object and get a map of claims with the path as the key and the value as the value It uses the DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim method and then selects the claims from the json object using the path
