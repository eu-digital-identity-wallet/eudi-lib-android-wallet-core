//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferManager](index.md)

# TransferManager

interface [TransferManager](index.md) : [TransferEvent.Listenable](../-transfer-event/-listenable/index.md)

Transfer manager interface for managing the transfer of data between the wallet and the reader.

#### Inheritors

| |
|---|
| [TransferManagerImpl](../-transfer-manager-impl/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md)<br>Companion object for creating a new instance of [TransferManager](index.md) |

## Properties

| Name | Summary |
|---|---|
| [requestProcessor](request-processor.md) | [release]<br>abstract val [requestProcessor](request-processor.md): [RequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/index.md) |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](../-transfer-event/-listenable/add-transfer-event-listener.md) | [release]<br>abstract fun [addTransferEventListener](../-transfer-event/-listenable/add-transfer-event-listener.md)(listener: [TransferEvent.Listener](../-transfer-event/-listener/index.md)): [TransferEvent.Listenable](../-transfer-event/-listenable/index.md)<br>Add transfer event listener |
| [removeAllTransferEventListeners](../-transfer-event/-listenable/remove-all-transfer-event-listeners.md) | [release]<br>abstract fun [removeAllTransferEventListeners](../-transfer-event/-listenable/remove-all-transfer-event-listeners.md)(): [TransferEvent.Listenable](../-transfer-event/-listenable/index.md)<br>Remove all transfer event listeners |
| [removeTransferEventListener](../-transfer-event/-listenable/remove-transfer-event-listener.md) | [release]<br>abstract fun [removeTransferEventListener](../-transfer-event/-listenable/remove-transfer-event-listener.md)(listener: [TransferEvent.Listener](../-transfer-event/-listener/index.md)): [TransferEvent.Listenable](../-transfer-event/-listenable/index.md)<br>Remove transfer event listener |
| [sendResponse](send-response.md) | [release]<br>abstract fun [sendResponse](send-response.md)(response: [Response](../../eu.europa.ec.eudi.iso18013.transfer.response/-response/index.md))<br>Sends response bytes to the connected reader and terminates the session. |
| [setRetrievalMethods](set-retrieval-methods.md) | [release]<br>abstract fun [setRetrievalMethods](set-retrieval-methods.md)(retrievalMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;): [TransferManager](index.md)<br>Set retrieval methods |
| [setupNfcEngagement](setup-nfc-engagement.md) | [release]<br>abstract fun [setupNfcEngagement](setup-nfc-engagement.md)(service: [NfcEngagementService](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-nfc-engagement-service/index.md)): [TransferManager](index.md)<br>Setup the [NfcEngagementService](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-nfc-engagement-service/index.md) Note: This method is only for internal use and should not be called by the app |
| [startQrEngagement](start-qr-engagement.md) | [release]<br>abstract fun [startQrEngagement](start-qr-engagement.md)()<br>Starts the QR Engagement and generates the QR code Once the QR code is ready, the event [TransferEvent.QrEngagementReady](../-transfer-event/-qr-engagement-ready/index.md) will be triggered |
| [stopPresentation](stop-presentation.md) | [release]<br>abstract fun [stopPresentation](stop-presentation.md)(sendSessionTerminationMessage: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = true, useTransportSpecificSessionTermination: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = false)<br>Closes the connection and clears the data of the session Also, sends a termination message if there is a connected verifier |