//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.statium](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [DefaultStatusReferenceExtractor](-default-status-reference-extractor/index.md) | [androidJvm]<br>object [DefaultStatusReferenceExtractor](-default-status-reference-extractor/index.md) : [StatusReferenceExtractor](-status-reference-extractor/index.md)<br>Default implementation of [StatusReferenceExtractor](-status-reference-extractor/index.md) It supports the following formats: |
| [DocumentStatusResolver](-document-status-resolver/index.md) | [androidJvm]<br>interface [DocumentStatusResolver](-document-status-resolver/index.md)<br>Interface for resolving the status of a document |
| [DocumentStatusResolverImpl](-document-status-resolver-impl/index.md) | [androidJvm]<br>class [DocumentStatusResolverImpl](-document-status-resolver-impl/index.md)(verifySignature: VerifyStatusListTokenJwtSignature, allowedClockSkew: [Duration](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.time/-duration/index.html), ktorHttpClientFactory: () -&gt; HttpClient, extractor: [StatusReferenceExtractor](-status-reference-extractor/index.md) = DefaultStatusReferenceExtractor, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : [DocumentStatusResolver](-document-status-resolver/index.md)<br>Default implementation of [DocumentStatusResolver](-document-status-resolver/index.md) |
| [MsoMdocStatusReferenceExtractor](-mso-mdoc-status-reference-extractor/index.md) | [androidJvm]<br>object [MsoMdocStatusReferenceExtractor](-mso-mdoc-status-reference-extractor/index.md) : [StatusReferenceExtractor](-status-reference-extractor/index.md)<br>Implements [StatusReferenceExtractor](-status-reference-extractor/index.md) for MSO MDOC format. Extracts the status reference from the issuerAuth data of the document. |
| [SdJwtStatusReferenceExtractor](-sd-jwt-status-reference-extractor/index.md) | [androidJvm]<br>object [SdJwtStatusReferenceExtractor](-sd-jwt-status-reference-extractor/index.md) : [StatusReferenceExtractor](-status-reference-extractor/index.md)<br>Extracts the status reference from an SD-JWT VC. |
| [SignatureVerificationError](-signature-verification-error/index.md) | [androidJvm]<br>class [SignatureVerificationError](-signature-verification-error/index.md) : [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html)<br>Custom exception for signature verification errors. |
| [StatusReferenceExtractor](-status-reference-extractor/index.md) | [androidJvm]<br>fun interface [StatusReferenceExtractor](-status-reference-extractor/index.md)<br>Interface for extracting revocation status data from documents |
| [VerifyStatusListTokenSignatureX5c](-verify-status-list-token-signature-x5c/index.md) | [androidJvm]<br>class [VerifyStatusListTokenSignatureX5c](-verify-status-list-token-signature-x5c/index.md) : VerifyStatusListTokenJwtSignature<br>Verifies the signature of a status list token using the x5c header. |

## Properties

| Name | Summary |
|---|---|
| [x5c](x5c.md) | [androidJvm]<br>val VerifyStatusListTokenJwtSignature.Companion.[x5c](x5c.md): VerifyStatusListTokenJwtSignature<br>Companion object for VerifyStatusListTokenSignature to provide a x5c implementation. |
