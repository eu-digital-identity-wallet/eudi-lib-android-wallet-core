//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../../index.md)/[TransferEvent](../index.md)/[RequestReceived](index.md)

# RequestReceived

[release]\
data class [RequestReceived](index.md)(val processedRequest: [RequestProcessor.ProcessedRequest](../../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md), val request: [Request](../../../eu.europa.ec.eudi.iso18013.transfer.response/-request/index.md)) : [TransferEvent](../index.md)

Request received event. This event is triggered when the request is received.

## Constructors

| | |
|---|---|
| [RequestReceived](-request-received.md) | [release]<br>constructor(processedRequest: [RequestProcessor.ProcessedRequest](../../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md), request: [Request](../../../eu.europa.ec.eudi.iso18013.transfer.response/-request/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [processedRequest](processed-request.md) | [release]<br>val [processedRequest](processed-request.md): [RequestProcessor.ProcessedRequest](../../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md)<br>the processed request containing the requested documents |
| [request](request.md) | [release]<br>val [request](request.md): [Request](../../../eu.europa.ec.eudi.iso18013.transfer.response/-request/index.md)<br>the request containing the raw data received |