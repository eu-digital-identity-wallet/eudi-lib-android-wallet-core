//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[ProcessedMsoMdocOpenId4VpRequest](index.md)

# ProcessedMsoMdocOpenId4VpRequest

class [ProcessedMsoMdocOpenId4VpRequest](index.md)(processedDeviceRequest: ProcessedDeviceRequest, resolvedRequestObject: ResolvedRequestObject, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : RequestProcessor.ProcessedRequest.Success

Processes and handles OpenID4VP requests specifically for MSO_MDOC document format.

This class extends RequestProcessor.ProcessedRequest.Success and specializes in handling OpenID for Verifiable Presentation (OpenID4VP) requests that exclusively target MSO_MDOC (Mobile Security Object Mobile Driving License) document formats. It transforms the processed device request into an OpenID4VP response that conforms to the Presentation Exchange protocol.

The class is responsible for:

- 
   Converting device responses to OpenID4VP verifiable presentations
- 
   Creating presentation submissions based on input descriptors
- 
   Generating Base64-encoded responses suitable for OpenID4VP communication

#### Parameters

androidJvm

| | |
|---|---|
| processedDeviceRequest | The device request that has been processed and is ready for response generation |
| resolvedRequestObject | The resolved OpenID4VP request object containing presentation query information |
| msoMdocNonce | A nonce value used for the MSO_MDOC protocol to prevent replay attacks |

## Constructors

| | |
|---|---|
| [ProcessedMsoMdocOpenId4VpRequest](-processed-mso-mdoc-open-id4-vp-request.md) | [androidJvm]<br>constructor(processedDeviceRequest: ProcessedDeviceRequest, resolvedRequestObject: ResolvedRequestObject, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [requestedDocuments](index.md#1436173325%2FProperties%2F1615067946) | [androidJvm]<br>val [requestedDocuments](index.md#1436173325%2FProperties%2F1615067946): RequestedDocuments |

## Functions

| Name | Summary |
|---|---|
| [generateResponse](generate-response.md) | [androidJvm]<br>open override fun [generateResponse](generate-response.md)(disclosedDocuments: DisclosedDocuments, signatureAlgorithm: Algorithm?): ResponseResult<br>Generates an OpenID4VP response from the disclosed documents. |
| [getOrNull](index.md#1268647320%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrNull](index.md#1268647320%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success? |
| [getOrThrow](index.md#-927339947%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrThrow](index.md#-927339947%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success |
