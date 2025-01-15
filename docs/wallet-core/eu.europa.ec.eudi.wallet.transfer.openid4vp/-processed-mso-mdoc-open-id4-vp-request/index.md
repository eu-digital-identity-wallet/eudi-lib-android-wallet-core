//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[ProcessedMsoMdocOpenId4VpRequest](index.md)

# ProcessedMsoMdocOpenId4VpRequest

[androidJvm]\
class [ProcessedMsoMdocOpenId4VpRequest](index.md)(processedDeviceRequest: ProcessedDeviceRequest, resolvedRequestObject: ResolvedRequestObject, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : RequestProcessor.ProcessedRequest.Success

## Constructors

| | |
|---|---|
| [ProcessedMsoMdocOpenId4VpRequest](-processed-mso-mdoc-open-id4-vp-request.md) | [androidJvm]<br>constructor(processedDeviceRequest: ProcessedDeviceRequest, resolvedRequestObject: ResolvedRequestObject, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [requestedDocuments](../-processed-generic-open-id4-vp-request/index.md#1436173325%2FProperties%2F1615067946) | [androidJvm]<br>val [requestedDocuments](../-processed-generic-open-id4-vp-request/index.md#1436173325%2FProperties%2F1615067946): RequestedDocuments |

## Functions

| Name | Summary |
|---|---|
| [generateResponse](generate-response.md) | [androidJvm]<br>open override fun [generateResponse](generate-response.md)(disclosedDocuments: DisclosedDocuments, signatureAlgorithm: Algorithm?): ResponseResult |
| [getOrNull](../-processed-generic-open-id4-vp-request/index.md#1268647320%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrNull](../-processed-generic-open-id4-vp-request/index.md#1268647320%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success? |
| [getOrThrow](../-processed-generic-open-id4-vp-request/index.md#-927339947%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrThrow](../-processed-generic-open-id4-vp-request/index.md#-927339947%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success |
