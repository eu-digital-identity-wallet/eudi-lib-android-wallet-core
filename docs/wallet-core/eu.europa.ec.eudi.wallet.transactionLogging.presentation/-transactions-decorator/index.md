//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)

# TransactionsDecorator

class [TransactionsDecorator](index.md)(delegate: [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md), documentManager: DocumentManager, transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) : [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)

Decorator for [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) that adds transaction logging capabilities.

This class wraps an existing [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) instance and intercepts key operations like sending responses and stopping presentations to ensure that all relevant transaction details are logged via a [TransactionsListener](../-transactions-listener/index.md).

#### Parameters

androidJvm

| | |
|---|---|
| documentManager | The manager for accessing document details, passed to the [TransactionsListener](../-transactions-listener/index.md). |
| transactionLogger | The logger for persisting transaction logs, passed to the [TransactionsListener](../-transactions-listener/index.md). |
| logger | Optional logger for internal logging of the decorator and listener. |

## Constructors

| | |
|---|---|
| [TransactionsDecorator](-transactions-decorator.md) | [androidJvm]<br>constructor(delegate: [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md), documentManager: DocumentManager, transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) |

## Properties

| Name | Summary |
|---|---|
| [nfcEngagementServiceClass](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/nfc-engagement-service-class.md) | [androidJvm]<br>open override val [nfcEngagementServiceClass](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/nfc-engagement-service-class.md): [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? |
| [readerTrustStore](index.md#1164183302%2FProperties%2F1615067946) | [androidJvm]<br>open override var [readerTrustStore](index.md#1164183302%2FProperties%2F1615067946): ReaderTrustStore? |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](index.md#-1321081126%2FFunctions%2F1615067946) | [androidJvm]<br>open override fun [addTransferEventListener](index.md#-1321081126%2FFunctions%2F1615067946)(listener: TransferEvent.Listener): TransferEvent.Listenable |
| [disableNFCEngagement](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/disable-n-f-c-engagement.md) | [androidJvm]<br>open override fun [disableNFCEngagement](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/disable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)<br>Disable the NFC device engagement for the wallet. This method should be called in the activity's ComponentActivity.onPause method. |
| [enableNFCEngagement](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/enable-n-f-c-engagement.md) | [androidJvm]<br>open override fun [enableNFCEngagement](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/enable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)<br>Enable the NFC device engagement for the wallet. This method should be called in the activity's ComponentActivity.onResume method. |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): &lt;Error class: unknown class&gt;<br>Removes all transfer event listeners from the delegate [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and then re-adds the internal transactionListener. |
| [removeTransferEventListener](index.md#1314902509%2FFunctions%2F1615067946) | [androidJvm]<br>open override fun [removeTransferEventListener](index.md#1314902509%2FFunctions%2F1615067946)(listener: TransferEvent.Listener): TransferEvent.Listenable |
| [sendResponse](send-response.md) | [androidJvm]<br>open override fun [sendResponse](send-response.md)(response: Response)<br>Sends a response using the delegate [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and logs the outcome. |
| [startProximityPresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-proximity-presentation.md) | [androidJvm]<br>open override fun [startProximityPresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-proximity-presentation.md)()<br>Starts the proximity presentation. The QR code is available through the TransferEvent.QrEngagementReady event which is triggered almost immediately after calling this method. The wallet should display the QR code to the verifier in order to start the proximity presentation. |
| [startRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-remote-presentation.md) | [androidJvm]<br>open override fun [startRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-remote-presentation.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Start a remote presentation with the given intent The intent.data could either contain the URI of<br>[androidJvm]<br>open override fun [startRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-remote-presentation.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html), refererUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?)<br>Start a remote presentation with the given URI The URI could be either |
| [stopProximityPresentation](stop-proximity-presentation.md) | [androidJvm]<br>open override fun [stopProximityPresentation](stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Stops the proximity presentation using the delegate [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and ensures the transaction logging is appropriately finalized by calling [TransactionsListener.logStopped](../-transactions-listener/log-stopped.md). |
| [stopRemotePresentation](stop-remote-presentation.md) | [androidJvm]<br>open override fun [stopRemotePresentation](stop-remote-presentation.md)()<br>Stops the remote presentation using the delegate [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and ensures the transaction logging is appropriately finalized by calling [TransactionsListener.logStopped](../-transactions-listener/log-stopped.md). |
