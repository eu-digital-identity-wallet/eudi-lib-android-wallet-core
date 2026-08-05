//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferManagerImpl](index.md)/[sendResponse](send-response.md)

# sendResponse

[release]\
open override fun [sendResponse](send-response.md)(response: [Response](../../eu.europa.ec.eudi.iso18013.transfer.response/-response/index.md))

Sends the response bytes to the connected mdoc verifier and terminates the session.

**Note:** Each session supports a single request-response cycle. Sending a response terminates the presentation session; start a new session to perform another exchange. This is conformant with ISO/IEC 18013-5:2021 §9.1.1.4, which makes additional exchanges optional, not required.

To generate the response, use the [eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest.generateResponse](../../eu.europa.ec.eudi.iso18013.transfer.response.device/-processed-device-request/generate-response.md) that is provided by the [eu.europa.ec.eudi.iso18013.transfer.TransferEvent.RequestReceived](../-transfer-event/-request-received/index.md) event.

#### Parameters

release

| | |
|---|---|
| response | the response to send |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if the response is not a [DeviceResponse](../../eu.europa.ec.eudi.iso18013.transfer.response.device/-device-response/index.md) |