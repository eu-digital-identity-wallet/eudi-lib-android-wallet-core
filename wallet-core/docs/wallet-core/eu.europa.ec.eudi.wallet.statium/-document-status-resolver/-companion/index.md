//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../../index.md)/[DocumentStatusResolver](../index.md)/[Companion](index.md)

# Companion

[release]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | [release]<br>operator fun [invoke](invoke.md)(block: [DocumentStatusResolver.Builder](../-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)): [DocumentStatusResolver](../index.md)<br>Creates an instance of [DocumentStatusResolver](../index.md) using a builder<br>[release]<br>operator fun [invoke](invoke.md)(verifySignature: VerifyStatusListTokenJwtSignature = VerifyStatusListTokenJwtSignature.x5c, ktorHttpClientFactory: () -&gt; HttpClient = { HttpClient() }, allowedClockSkew: [Duration](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/index.html) = Duration.ZERO): [DocumentStatusResolver](../index.md)<br>Creates an instance of [DocumentStatusResolver](../index.md) |