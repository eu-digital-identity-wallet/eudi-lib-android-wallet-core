//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [DcqlRequestProcessor](-dcql-request-processor/index.md) | [androidJvm]<br>class [DcqlRequestProcessor](-dcql-request-processor/index.md)(documentManager: DocumentManager, var openid4VpX509CertificateTrust: [OpenId4VpReaderTrust](../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-reader-trust/index.md)) : RequestProcessor<br>Processes OpenID4VP requests using DCQL (Digital Credentials Query Language). |
| [ProcessedDcqlRequest](-processed-dcql-request/index.md) | [androidJvm]<br>class [ProcessedDcqlRequest](-processed-dcql-request/index.md)(val resolvedRequestObject: ResolvedRequestObject.OpenId4VPAuthorization, documentManager: DocumentManager, queryMap: [RequestedDocumentsByQueryId](-requested-documents-by-query-id/index.md), val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : RequestProcessor.ProcessedRequest.Success<br>Represents a processed DCQL (Digital Credentials Query Language) request for OpenID4VP flows. |
| [RequestedDocumentsByFormat](-requested-documents-by-format/index.md) | [androidJvm]<br>data class [RequestedDocumentsByFormat](-requested-documents-by-format/index.md)(val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val requestedDocuments: RequestedDocuments)<br>Data structure that associates a document format with a collection of requested documents. |
| [RequestedDocumentsByQueryId](-requested-documents-by-query-id/index.md) | [androidJvm]<br>typealias [RequestedDocumentsByQueryId](-requested-documents-by-query-id/index.md) = [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, [RequestedDocumentsByFormat](-requested-documents-by-format/index.md)&gt;<br>Type alias for a map that associates DCQL query identifiers with their corresponding documents and formats. |
