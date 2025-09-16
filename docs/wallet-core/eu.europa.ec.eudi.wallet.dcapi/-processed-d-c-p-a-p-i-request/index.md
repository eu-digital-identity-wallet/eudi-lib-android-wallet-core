//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[ProcessedDCPAPIRequest](index.md)

# ProcessedDCPAPIRequest

class [ProcessedDCPAPIRequest](index.md)(processedDeviceRequest: ProcessedDeviceRequest, providerGetCredentialRequest: [ProviderGetCredentialRequest](https://developer.android.com/reference/kotlin/androidx/credentials/provider/ProviderGetCredentialRequest.html), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, val origin: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), requestedDocuments: RequestedDocuments) : RequestProcessor.ProcessedRequest.Success

Processes a DCAPI request by generating a response based on the provided device request and credential options.

This implementation follows the protocol `org-iso-mdoc` as defined in the ISO/IEC TS 18013-7:2025 Annex C.

#### Parameters

androidJvm

| | |
|---|---|
| origin | The origin of the request. |

## Constructors

| | |
|---|---|
| [ProcessedDCPAPIRequest](-processed-d-c-p-a-p-i-request.md) | [androidJvm]<br>constructor(processedDeviceRequest: ProcessedDeviceRequest, providerGetCredentialRequest: [ProviderGetCredentialRequest](https://developer.android.com/reference/kotlin/androidx/credentials/provider/ProviderGetCredentialRequest.html), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, origin: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), requestedDocuments: RequestedDocuments) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [origin](origin.md) | [androidJvm]<br>val [origin](origin.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [requestedDocuments](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-processed-dcql-request/index.md#1436173325%2FProperties%2F1615067946) | [androidJvm]<br>val [requestedDocuments](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-processed-dcql-request/index.md#1436173325%2FProperties%2F1615067946): RequestedDocuments |

## Functions

| Name | Summary |
|---|---|
| [generateResponse](generate-response.md) | [androidJvm]<br>open override fun [generateResponse](generate-response.md)(disclosedDocuments: DisclosedDocuments, signatureAlgorithm: Algorithm?): ResponseResult |
| [getOrNull](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-processed-dcql-request/index.md#1268647320%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrNull](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-processed-dcql-request/index.md#1268647320%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success? |
| [getOrThrow](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-processed-dcql-request/index.md#-927339947%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrThrow](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-processed-dcql-request/index.md#-927339947%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success |
