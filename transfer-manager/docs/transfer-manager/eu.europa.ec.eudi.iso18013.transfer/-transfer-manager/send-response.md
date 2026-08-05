//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferManager](index.md)/[sendResponse](send-response.md)

# sendResponse

[release]\
abstract fun [sendResponse](send-response.md)(response: [Response](../../eu.europa.ec.eudi.iso18013.transfer.response/-response/index.md))

Sends response bytes to the connected reader and terminates the session.

**Note:** Each session supports a single request-response cycle. Sending a response terminates the presentation session; start a new session to perform another exchange. This is conformant with ISO/IEC 18013-5:2021 §9.1.1.4, which makes additional exchanges optional, not required.

To generate the response, use the [RequestProcessor.ProcessedRequest.Success.generateResponse](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-success/generate-response.md) method.

#### Parameters

release

| | |
|---|---|
| response | The response to be sent |