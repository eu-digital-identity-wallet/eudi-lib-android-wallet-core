//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../../index.md)/[DocumentStatusResolver](../index.md)/[Companion](index.md)/[invoke](invoke.md)

# invoke

[release]\
operator fun [invoke](invoke.md)(verifySignature: VerifyStatusListTokenJwtSignature = VerifyStatusListTokenJwtSignature.x5c, ktorHttpClientFactory: () -&gt; HttpClient = { HttpClient() }, allowedClockSkew: [Duration](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/index.html) = Duration.ZERO): [DocumentStatusResolver](../index.md)

Creates an instance of [DocumentStatusResolver](../index.md)

#### Parameters

release

| | |
|---|---|
| ktorHttpClientFactory | a factory function to create an HttpClient |
| verifySignature | a function to verify the status list token signature |
| allowedClockSkew | the allowed clock skew for the verification |

[release]\
operator fun [invoke](invoke.md)(block: [DocumentStatusResolver.Builder](../-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)): [DocumentStatusResolver](../index.md)

Creates an instance of [DocumentStatusResolver](../index.md) using a builder

#### Return

a [DocumentStatusResolver](../index.md) instance

#### Parameters

release

| | |
|---|---|
| block | a lambda function with a [Builder](../-builder/index.md) as receiver to configure the resolver |