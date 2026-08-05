//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../index.md)/[ProcessedDeviceRequest](index.md)

# ProcessedDeviceRequest

[release]\
class [ProcessedDeviceRequest](index.md)(documentManager: DocumentManager, sessionTranscript: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), requestedDocuments: [RequestedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md), zkSystemRepository: ZkSystemRepository? = null, readerAuthPolicy: [ReaderAuthPolicy](../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md) = ReaderAuthPolicy.EnforceIfPresent, zkResponsePolicy: [ZkResponsePolicy](../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md) = ZkResponsePolicy.FallbackToFullDisclosure) : [RequestProcessor.ProcessedRequest.Success](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/index.md)

Implementation of [RequestProcessor.ProcessedRequest.Success](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/index.md) for [DeviceRequest](../-device-request/index.md).

## Constructors

| | |
|---|---|
| [ProcessedDeviceRequest](-processed-device-request.md) | [release]<br>constructor(documentManager: DocumentManager, sessionTranscript: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), requestedDocuments: [RequestedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md), zkSystemRepository: ZkSystemRepository? = null, readerAuthPolicy: [ReaderAuthPolicy](../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md) = ReaderAuthPolicy.EnforceIfPresent, zkResponsePolicy: [ZkResponsePolicy](../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md) = ZkResponsePolicy.FallbackToFullDisclosure) |

## Properties

| Name | Summary |
|---|---|
| [requestedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/requested-documents.md) | [release]<br>val [requestedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/requested-documents.md): [RequestedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md)<br>the requested documents |

## Functions

| Name | Summary |
|---|---|
| [generateResponse](generate-response.md) | [release]<br>open override fun [generateResponse](generate-response.md)(disclosedDocuments: [DisclosedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-disclosed-documents/index.md), signatureAlgorithm: Algorithm? = null): [ResponseResult](../../eu.europa.ec.eudi.iso18013.transfer.response/-response-result/index.md)<br>Generate the response for the disclosed documents. |
| [getOrNull](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/get-or-null.md) | [release]<br>open fun [getOrNull](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/get-or-null.md)(): [RequestProcessor.ProcessedRequest.Success](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/index.md)?<br>Returns the processed request or null |
| [getOrThrow](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/get-or-throw.md) | [release]<br>open fun [getOrThrow](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/get-or-throw.md)(): [RequestProcessor.ProcessedRequest.Success](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/index.md)<br>Returns the processed request or throws the error |
| [toKotlinResult](../../eu.europa.ec.eudi.iso18013.transfer/to-kotlin-result.md) | [release]<br>fun [RequestProcessor.ProcessedRequest](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md).[toKotlinResult](../../eu.europa.ec.eudi.iso18013.transfer/to-kotlin-result.md)(): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[RequestProcessor.ProcessedRequest.Success](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/index.md)&gt;<br>Converts a [RequestProcessor.ProcessedRequest](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md) to a [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) of [RequestProcessor.ProcessedRequest.Success](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/index.md) |