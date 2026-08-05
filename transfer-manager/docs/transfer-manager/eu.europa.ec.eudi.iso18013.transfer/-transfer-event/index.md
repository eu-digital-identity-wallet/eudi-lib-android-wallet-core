//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferEvent](index.md)

# TransferEvent

sealed interface [TransferEvent](index.md)

Transfer event

#### Inheritors

| |
|---|
| [QrEngagementReady](-qr-engagement-ready/index.md) |
| [Connecting](-connecting/index.md) |
| [Connected](-connected/index.md) |
| [RequestReceived](-request-received/index.md) |
| [ResponseSent](-response-sent/index.md) |
| [Redirect](-redirect/index.md) |
| [IntentToSend](-intent-to-send/index.md) |
| [Disconnected](-disconnected/index.md) |
| [Error](-error/index.md) |

## Types

| Name | Summary |
|---|---|
| [Connected](-connected/index.md) | [release]<br>data object [Connected](-connected/index.md) : [TransferEvent](index.md)<br>Connected event. This event is triggered when the transfer is connected. |
| [Connecting](-connecting/index.md) | [release]<br>data object [Connecting](-connecting/index.md) : [TransferEvent](index.md)<br>Connecting event. This event is triggered when the transfer is connecting. |
| [Disconnected](-disconnected/index.md) | [release]<br>data object [Disconnected](-disconnected/index.md) : [TransferEvent](index.md)<br>Disconnected event. This event is triggered when the transfer is disconnected. |
| [Error](-error/index.md) | [release]<br>data class [Error](-error/index.md)(val error: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) : [TransferEvent](index.md)<br>Error event. This event is triggered when an error occurs. |
| [IntentToSend](-intent-to-send/index.md) | [release]<br>data class [IntentToSend](-intent-to-send/index.md)(val intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html)) : [TransferEvent](index.md)<br>Intent to send event. This event is triggered when an intent is to be sent. This event is to be used for implementation of Digital Credentials API. |
| [Listenable](-listenable/index.md) | [release]<br>interface [Listenable](-listenable/index.md)<br>Interface for events listenable |
| [Listener](-listener/index.md) | [release]<br>fun interface [Listener](-listener/index.md)<br>Interface for transfer event listener |
| [QrEngagementReady](-qr-engagement-ready/index.md) | [release]<br>data class [QrEngagementReady](-qr-engagement-ready/index.md)(val qrCode: [QrCode](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-qr-code/index.md)) : [TransferEvent](index.md)<br>Qr engagement ready event. This event is triggered when the QR code is ready to be displayed. |
| [Redirect](-redirect/index.md) | [release]<br>data class [Redirect](-redirect/index.md)(val redirectUri: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)) : [TransferEvent](index.md)<br>Redirect event. This event is triggered when a redirect is needed. This event is to be used for implementation for the OpenId4VP protocol. |
| [RequestReceived](-request-received/index.md) | [release]<br>data class [RequestReceived](-request-received/index.md)(val processedRequest: [RequestProcessor.ProcessedRequest](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md), val request: [Request](../../eu.europa.ec.eudi.iso18013.transfer.response/-request/index.md)) : [TransferEvent](index.md)<br>Request received event. This event is triggered when the request is received. |
| [ResponseSent](-response-sent/index.md) | [release]<br>data object [ResponseSent](-response-sent/index.md) : [TransferEvent](index.md)<br>Response sent event. This event is triggered when the response is sent. |