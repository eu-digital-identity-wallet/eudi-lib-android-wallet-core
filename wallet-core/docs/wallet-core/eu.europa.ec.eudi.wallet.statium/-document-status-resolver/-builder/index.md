//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../../index.md)/[DocumentStatusResolver](../index.md)/[Builder](index.md)

# Builder

[release]\
class [Builder](index.md)

Builder for [DocumentStatusResolver](../index.md) It allows to set the parameters for the resolver it builds a [DocumentStatusResolverImpl](../../-document-status-resolver-impl/index.md)

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [allowedClockSkew](allowed-clock-skew.md) | [release]<br>var [allowedClockSkew](allowed-clock-skew.md): [Duration](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/index.html)<br>the allowed clock skew for the verification; default is [Duration.ZERO](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/-companion/-z-e-r-o.html) |
| [extractor](extractor.md) | [release]<br>var [extractor](extractor.md): [StatusReferenceExtractor](../../-status-reference-extractor/index.md)<br>an instance of [StatusReferenceExtractor](../../-status-reference-extractor/index.md) to extract the status reference from the document; default is [DefaultStatusReferenceExtractor](../../-default-status-reference-extractor/index.md) |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [release]<br>var [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient<br>a factory function to create an HttpClient; default is HttpClient |
| [verifySignature](verify-signature.md) | [release]<br>var [verifySignature](verify-signature.md): VerifyStatusListTokenJwtSignature<br>a function to verify the status list token signature; default is VerifyStatusListTokenJwtSignature.x5c |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [DocumentStatusResolver](../index.md)<br>Builds the [DocumentStatusResolver](../index.md) instance |
| [withAllowedClockSkew](with-allowed-clock-skew.md) | [release]<br>fun [withAllowedClockSkew](with-allowed-clock-skew.md)(allowedClockSkew: [Duration](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/index.html)): [DocumentStatusResolver.Builder](index.md)<br>Sets the allowed clock skew for the verification |
| [withExtractor](with-extractor.md) | [release]<br>fun [withExtractor](with-extractor.md)(extractor: [StatusReferenceExtractor](../../-status-reference-extractor/index.md)): [DocumentStatusResolver.Builder](index.md)<br>Sets the instance of [StatusReferenceExtractor](../../-status-reference-extractor/index.md) to extract the status reference from the document |
| [withKtorHttpClientFactory](with-ktor-http-client-factory.md) | [release]<br>fun [withKtorHttpClientFactory](with-ktor-http-client-factory.md)(ktorHttpClientFactory: () -&gt; HttpClient): [DocumentStatusResolver.Builder](index.md)<br>Sets the factory function to create an HttpClient |
| [withVerifySignature](with-verify-signature.md) | [release]<br>fun [withVerifySignature](with-verify-signature.md)(verifySignature: VerifyStatusListTokenJwtSignature): [DocumentStatusResolver.Builder](index.md)<br>Sets the function to verify the status list token signature |