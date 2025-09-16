//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManager](index.md)

# PresentationManager

interface [PresentationManager](index.md) : TransferEvent.Listenable, ReaderTrustStoreAware

The PresentationManager is responsible for managing the presentation of the wallet's documents to the verifier. The wallet can present the documents in two ways:

- 
   Proximity presentation: the wallet uses BLE/NFC to present the documents to verifier's device
- 
   Remote presentation: the wallet sends the documents to the verifier's server

This interface extends TransferEvent.Listenable that allows to listen to the transfer events through which it receives the requests from the verifier. After receiving the eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest the wallet can generate the response with eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest.Success.generateResponse and send it back to the verifier by calling [sendResponse](send-response.md) method.

It also extends ReaderTrustStoreAware that allows to set the ReaderTrustStore that is used to verify the authenticity of the reader.

It provides also functionality to start the NFC engagement by calling [enableNFCEngagement](enable-n-f-c-engagement.md) method and stop it by calling [disableNFCEngagement](disable-n-f-c-engagement.md) method.

#### Inheritors

| |
|---|
| [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) |
| [EudiWalletImpl](../../eu.europa.ec.eudi.wallet/-eudi-wallet-impl/index.md) |
| [PresentationManagerImpl](../-presentation-manager-impl/index.md) |
| [TransactionsDecorator](../../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-transactions-decorator/index.md) |

## Properties

| Name | Summary |
|---|---|
| [nfcEngagementServiceClass](nfc-engagement-service-class.md) | [androidJvm]<br>abstract val [nfcEngagementServiceClass](nfc-engagement-service-class.md): [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? |
| [readerTrustStore](../../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-transactions-decorator/index.md#1164183302%2FProperties%2F1615067946) | [androidJvm]<br>abstract var [readerTrustStore](../../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-transactions-decorator/index.md#1164183302%2FProperties%2F1615067946): ReaderTrustStore? |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](../../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-transactions-decorator/index.md#-1321081126%2FFunctions%2F1615067946) | [androidJvm]<br>abstract fun [addTransferEventListener](../../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-transactions-decorator/index.md#-1321081126%2FFunctions%2F1615067946)(listener: TransferEvent.Listener): TransferEvent.Listenable |
| [disableNFCEngagement](disable-n-f-c-engagement.md) | [androidJvm]<br>abstract fun [disableNFCEngagement](disable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManager](index.md)<br>Disable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onPause](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html#onpause) method. |
| [enableNFCEngagement](enable-n-f-c-engagement.md) | [androidJvm]<br>abstract fun [enableNFCEngagement](enable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManager](index.md)<br>Enable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onResume](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html#onresume) method. |
| [removeAllTransferEventListeners](index.md#-812467576%2FFunctions%2F1615067946) | [androidJvm]<br>abstract fun [removeAllTransferEventListeners](index.md#-812467576%2FFunctions%2F1615067946)(): TransferEvent.Listenable |
| [removeTransferEventListener](../../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-transactions-decorator/index.md#1314902509%2FFunctions%2F1615067946) | [androidJvm]<br>abstract fun [removeTransferEventListener](../../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-transactions-decorator/index.md#1314902509%2FFunctions%2F1615067946)(listener: TransferEvent.Listener): TransferEvent.Listenable |
| [sendResponse](send-response.md) | [androidJvm]<br>abstract fun [sendResponse](send-response.md)(response: Response)<br>Send a response to verifier |
| [startDCAPIPresentation](start-d-c-a-p-i-presentation.md) | [androidJvm]<br>abstract fun [startDCAPIPresentation](start-d-c-a-p-i-presentation.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Starts the DCAPI presentation. This method is used to start the DCAPI presentation flow. |
| [startProximityPresentation](start-proximity-presentation.md) | [androidJvm]<br>abstract fun [startProximityPresentation](start-proximity-presentation.md)()<br>Starts the proximity presentation. The QR code is available through the TransferEvent.QrEngagementReady event which is triggered almost immediately after calling this method. The wallet should display the QR code to the verifier in order to start the proximity presentation. |
| [startRemotePresentation](start-remote-presentation.md) | [androidJvm]<br>abstract fun [startRemotePresentation](start-remote-presentation.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Start a remote presentation with the given intent The intent.data could either contain the URI of<br>[androidJvm]<br>abstract fun [startRemotePresentation](start-remote-presentation.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html), refererUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null)<br>Start a remote presentation with the given URI The URI could be either |
| [stopProximityPresentation](stop-proximity-presentation.md) | [androidJvm]<br>abstract fun [stopProximityPresentation](stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = SEND_SESSION_TERMINATION_MESSAGE)<br>Stops the proximity presentation. Method receives flags that can be used to control the session termination. The available flags are: |
| [stopRemotePresentation](stop-remote-presentation.md) | [androidJvm]<br>abstract fun [stopRemotePresentation](stop-remote-presentation.md)()<br>Stops any ongoing remote presentation |
