//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferManagerImpl](index.md)

# TransferManagerImpl

class [TransferManagerImpl](index.md)@[JvmOverloads](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), val requestProcessor: [RequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/index.md), retrievalMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;? = null) : [TransferManager](../-transfer-manager/index.md), [ReaderTrustStoreAware](../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store-aware/index.md)

Transfer Manager class used for performing device engagement and data retrieval for ISO 18013-5 standard.

#### Parameters

release

| | |
|---|---|
| context | application context |
| requestProcessor | request processor for processing the device request and generating the response |
| retrievalMethods | list of device retrieval methods to be used for the transfer |

## Constructors

| | |
|---|---|
| [TransferManagerImpl](-transfer-manager-impl.md) | [release]<br>@[JvmOverloads](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), requestProcessor: [RequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/index.md), retrievalMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;? = null)<br>Create a Transfer Manager |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [release]<br>class [Builder](-builder/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))<br>Builder class for instantiating a [TransferManager](../-transfer-manager/index.md) implementation |
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md)<br>Companion object for creating a new instance of [TransferManager](../-transfer-manager/index.md) |

## Properties

| Name | Summary |
|---|---|
| [readerTrustStore](reader-trust-store.md) | [release]<br>open override var [readerTrustStore](reader-trust-store.md): [ReaderTrustStore](../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store/index.md)?<br>the reader trust store |
| [requestProcessor](request-processor.md) | [release]<br>open override val [requestProcessor](request-processor.md): [RequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/index.md) |
| [retrievalMethods](retrieval-methods.md) | [release]<br>var [retrievalMethods](retrieval-methods.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;<br>List of device retrieval methods |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [release]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: [TransferEvent.Listener](../-transfer-event/-listener/index.md)): [TransferManagerImpl](index.md)<br>Add a transfer event listener |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [release]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [TransferManagerImpl](index.md)<br>Remove all transfer event listeners |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [release]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: [TransferEvent.Listener](../-transfer-event/-listener/index.md)): [TransferManagerImpl](index.md)<br>Remove a transfer event listener |
| [sendResponse](send-response.md) | [release]<br>open override fun [sendResponse](send-response.md)(response: [Response](../../eu.europa.ec.eudi.iso18013.transfer.response/-response/index.md))<br>Sends the response bytes to the connected mdoc verifier and terminates the session. |
| [setRetrievalMethods](set-retrieval-methods.md) | [release]<br>open override fun [setRetrievalMethods](set-retrieval-methods.md)(retrievalMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;): [TransferManagerImpl](index.md)<br>Set retrieval methods |
| [setupNfcEngagement](setup-nfc-engagement.md) | [release]<br>open override fun [setupNfcEngagement](setup-nfc-engagement.md)(service: [NfcEngagementService](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-nfc-engagement-service/index.md)): [TransferManagerImpl](index.md)<br>Sets up NFC engagement with the provided service Note: This method is only for internal use and should not be called by the app |
| [startQrEngagement](start-qr-engagement.md) | [release]<br>open override fun [startQrEngagement](start-qr-engagement.md)()<br>Starts the QR Engagement and generates the QR code Once the QR code is ready, the event [TransferEvent.QrEngagementReady](../-transfer-event/-qr-engagement-ready/index.md) will be triggered If the transfer has already started, an error event will be triggered with an [IllegalStateException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-state-exception/index.html) containing the message &quot;Transfer has already started.&quot; |
| [stopPresentation](stop-presentation.md) | [release]<br>open override fun [stopPresentation](stop-presentation.md)(sendSessionTerminationMessage: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = true, useTransportSpecificSessionTermination: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = false)<br>Closes the connection and clears the data of the session Also, sends a termination message if there is a connected mdoc verifier |