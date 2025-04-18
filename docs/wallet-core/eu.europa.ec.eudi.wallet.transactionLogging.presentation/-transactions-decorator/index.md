//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)

# TransactionsDecorator

[androidJvm]\
class [TransactionsDecorator](index.md)(delegate: [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md), documentManager: DocumentManager, transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) : [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)

Decorator for [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) that logs transactions.

This class is responsible for logging the transactions that are sent through the [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md). It wraps the original [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and adds logging functionality to it.

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
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): &lt;Error class: unknown class&gt;<br>Removes all transfer event listeners and adds the transaction listener. |
| [removeTransferEventListener](index.md#1314902509%2FFunctions%2F1615067946) | [androidJvm]<br>open override fun [removeTransferEventListener](index.md#1314902509%2FFunctions%2F1615067946)(listener: TransferEvent.Listener): TransferEvent.Listenable |
| [sendResponse](send-response.md) | [androidJvm]<br>open override fun [sendResponse](send-response.md)(response: Response)<br>Sends a response and logs it. |
| [startProximityPresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-proximity-presentation.md) | [androidJvm]<br>open override fun [startProximityPresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-proximity-presentation.md)()<br>Starts the proximity presentation. The QR code is available through the TransferEvent.QrEngagementReady event which is triggered almost immediately after calling this method. The wallet should display the QR code to the verifier in order to start the proximity presentation. |
| [startRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-remote-presentation.md) | [androidJvm]<br>open override fun [startRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-remote-presentation.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Start a remote presentation with the given intent The intent.data could either contain the URI of<br>[androidJvm]<br>open override fun [startRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/start-remote-presentation.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html), refererUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?)<br>Start a remote presentation with the given URI The URI could be either |
| [stopProximityPresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/stop-proximity-presentation.md) | [androidJvm]<br>open override fun [stopProximityPresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Stops the proximity presentation. Method receives flags that can be used to control the session termination. The available flags are: |
| [stopRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/stop-remote-presentation.md) | [androidJvm]<br>open override fun [stopRemotePresentation](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/stop-remote-presentation.md)()<br>Stops any ongoing remote presentation |
