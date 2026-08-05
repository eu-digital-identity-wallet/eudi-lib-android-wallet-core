//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../index.md)/[DeviceRequestProcessor](index.md)/[process](process.md)

# process

[release]\
open override fun [process](process.md)(request: [Request](../../eu.europa.ec.eudi.iso18013.transfer.response/-request/index.md)): [RequestProcessor.ProcessedRequest](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md)

Process the [DeviceRequest](../-device-request/index.md) and return the [ProcessedDeviceRequest](../-processed-device-request/index.md) or a [RequestProcessor.ProcessedRequest.Failure](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-failure/index.md).

#### Return

the [ProcessedDeviceRequest](../-processed-device-request/index.md) or a [RequestProcessor.ProcessedRequest.Failure](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-failure/index.md)

#### Parameters

release

| | |
|---|---|
| request | the [DeviceRequest](../-device-request/index.md) to process |