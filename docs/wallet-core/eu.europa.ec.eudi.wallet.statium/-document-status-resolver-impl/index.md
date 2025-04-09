//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[DocumentStatusResolverImpl](index.md)

# DocumentStatusResolverImpl

class [DocumentStatusResolverImpl](index.md)(verifySignature: VerifyStatusListTokenSignature, allowedClockSkew: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.time/-duration/index.html), ktorHttpClientFactory: () -&gt; HttpClient, extractor: [StatusReferenceExtractor](../-status-reference-extractor/index.md) = DefaultStatusReferenceExtractor) : [DocumentStatusResolver](../-document-status-resolver/index.md)

Default implementation of [DocumentStatusResolver](../-document-status-resolver/index.md)

#### Parameters

androidJvm

| | |
|---|---|
| verifySignature | a function to verify the status list token signature |
| allowedClockSkew | the allowed clock skew for the verification |
| ktorHttpClientFactory | a factory function to create an HttpClient |
| extractor | an instance of [StatusReferenceExtractor](../-status-reference-extractor/index.md) to extract the status reference from the document |

## Constructors

| | |
|---|---|
| [DocumentStatusResolverImpl](-document-status-resolver-impl.md) | [androidJvm]<br>constructor(verifySignature: VerifyStatusListTokenSignature, allowedClockSkew: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.time/-duration/index.html), ktorHttpClientFactory: () -&gt; HttpClient, extractor: [StatusReferenceExtractor](../-status-reference-extractor/index.md) = DefaultStatusReferenceExtractor) |

## Functions

| Name | Summary |
|---|---|
| [resolveStatus](resolve-status.md) | [androidJvm]<br>open suspend override fun [resolveStatus](resolve-status.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;Status&gt;<br>Resolves the status of the given document |
