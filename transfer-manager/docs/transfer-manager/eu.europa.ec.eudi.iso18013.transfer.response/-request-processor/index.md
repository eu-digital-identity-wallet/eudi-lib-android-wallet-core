//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[RequestProcessor](index.md)

# RequestProcessor

fun interface [RequestProcessor](index.md)

Interface for request processor. A request processor processes the raw request and returns a processed request. The processed request can be either a success or a failure. If the processed request is a success, the requested documents are returned and the response can be generated. If the processed request is a failure, the error is returned.

#### Inheritors

| |
|---|
| [DeviceRequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response.device/-device-request-processor/index.md) |

## Types

| Name | Summary |
|---|---|
| [ProcessedRequest](-processed-request/index.md) | [release]<br>sealed interface [ProcessedRequest](-processed-request/index.md)<br>Represents the result of a processed request |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [release]<br>abstract fun [process](process.md)(request: [Request](../-request/index.md)): [RequestProcessor.ProcessedRequest](-processed-request/index.md)<br>Processes the request |