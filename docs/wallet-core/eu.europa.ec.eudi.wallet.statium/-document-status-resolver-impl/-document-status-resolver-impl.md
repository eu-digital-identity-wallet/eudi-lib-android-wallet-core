//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[DocumentStatusResolverImpl](index.md)/[DocumentStatusResolverImpl](-document-status-resolver-impl.md)

# DocumentStatusResolverImpl

[androidJvm]\
constructor(verifySignature: VerifyStatusListTokenSignature, allowedClockSkew: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.time/-duration/index.html), ktorHttpClientFactory: () -&gt; HttpClient, extractor: [StatusReferenceExtractor](../-status-reference-extractor/index.md) = DefaultStatusReferenceExtractor)

#### Parameters

androidJvm

| | |
|---|---|
| verifySignature | a function to verify the status list token signature |
| allowedClockSkew | the allowed clock skew for the verification |
| ktorHttpClientFactory | a factory function to create an HttpClient |
| extractor | an instance of [StatusReferenceExtractor](../-status-reference-extractor/index.md) to extract the status reference from the document |
