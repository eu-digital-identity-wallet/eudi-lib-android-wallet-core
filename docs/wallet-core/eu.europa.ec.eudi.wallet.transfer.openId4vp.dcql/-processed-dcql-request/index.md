//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[ProcessedDcqlRequest](index.md)

# ProcessedDcqlRequest

[androidJvm]\
class [ProcessedDcqlRequest](index.md)(val resolvedRequestObject: ResolvedRequestObject, documentManager: DocumentManager, queryMap: [RequestedDocumentsByQueryId](../-requested-documents-by-query-id/index.md), val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : RequestProcessor.ProcessedRequest.Success

Represents a processed DCQL (Digital Credentials Query Language) request for OpenID4VP flows.

After a DCQL request has been processed by the [DcqlRequestProcessor](../-dcql-request-processor/index.md), this class holds the results and is responsible for generating appropriate responses when the user selects which documents to disclose. It supports multiple document formats and ensures proper response formatting.

This class:

1. 
   Organizes requested documents by query ID and document format
2. 
   Generates verifiable presentations for selected documents
3. 
   Ensures only one document per query is disclosed (per protocol requirements)
4. 
   Constructs properly formatted OpenID4VP responses

## Constructors

| | |
|---|---|
| [ProcessedDcqlRequest](-processed-dcql-request.md) | [androidJvm]<br>constructor(resolvedRequestObject: ResolvedRequestObject, documentManager: DocumentManager, queryMap: [RequestedDocumentsByQueryId](../-requested-documents-by-query-id/index.md), msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>Random nonce used for MSO mdoc format presentations for security |
| [requestedDocuments](index.md#1436173325%2FProperties%2F1615067946) | [androidJvm]<br>val [requestedDocuments](index.md#1436173325%2FProperties%2F1615067946): RequestedDocuments |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject<br>The parsed OpenID4VP authorization request with presentation query details |

## Functions

| Name | Summary |
|---|---|
| [generateResponse](generate-response.md) | [androidJvm]<br>open override fun [generateResponse](generate-response.md)(disclosedDocuments: DisclosedDocuments, signatureAlgorithm: Algorithm?): ResponseResult<br>Generates an OpenID4VP response with verifiable presentations for the selected documents. |
| [getOrNull](index.md#1268647320%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrNull](index.md#1268647320%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success? |
| [getOrThrow](index.md#-927339947%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrThrow](index.md#-927339947%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success |
