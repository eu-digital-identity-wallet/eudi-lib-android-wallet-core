//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManager](index.md)

# PresentationManager

interface [PresentationManager](index.md) : TransferEvent.Listenable, ReaderTrustStoreAware

The PresentationManager is responsible for managing the presentation of the wallet's documents to
the verifier. The wallet can present the documents in two ways:

-
Proximity presentation: the wallet uses BLE/NFC to present the documents to verifier's device
-
Remote presentation: the wallet sends the documents to the verifier's server

This interface extends TransferEvent.Listenable that allows to listen to the transfer events through
which it receives the requests from the verifier. After receiving the
eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest the wallet can
generate the response with
eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest.Success.generateResponse
and send it back to the verifier by calling [sendResponse](send-response.md) method.

It also extends ReaderTrustStoreAware that allows to set the ReaderTrustStore that is used to verify
the authenticity of the reader.

It provides also functionality to start the NFC engagement by
calling [enableNFCEngagement](enable-n-f-c-engagement.md) method and stop it by
calling [disableNFCEngagement](disable-n-f-c-engagement.md) method.

#### Inheritors

|                                                                             |
|-----------------------------------------------------------------------------|
| [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md)          |
| [EudiWalletImpl](../../eu.europa.ec.eudi.wallet/-eudi-wallet-impl/index.md) |
| [PresentationManagerImpl](../-presentation-manager-impl/index.md)           |

## Properties

| Name                                                              | Summary                                                                                                                                                                                              |
|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [nfcEngagementServiceClass](nfc-engagement-service-class.md)      | [androidJvm]<br>abstract val [nfcEngagementServiceClass](nfc-engagement-service-class.md): [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;NfcEngagementService&gt;? |
| [readerTrustStore](index.md#1164183302%2FProperties%2F1615067946) | [androidJvm]<br>abstract var [readerTrustStore](index.md#1164183302%2FProperties%2F1615067946): ReaderTrustStore?                                                                                    |

## Functions

| Name                                                                            | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|---------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [addTransferEventListener](index.md#-1321081126%2FFunctions%2F1615067946)       | [androidJvm]<br>abstract fun [addTransferEventListener](index.md#-1321081126%2FFunctions%2F1615067946)(listener: TransferEvent.Listener): TransferEvent.Listenable                                                                                                                                                                                                                                                                                                                 |
| [disableNFCEngagement](disable-n-f-c-engagement.md)                             | [androidJvm]<br>abstract fun [disableNFCEngagement](disable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManager](index.md)<br>Disable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onPause](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html#onpause) method. |
| [enableNFCEngagement](enable-n-f-c-engagement.md)                               | [androidJvm]<br>abstract fun [enableNFCEngagement](enable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManager](index.md)<br>Enable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onResume](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html#onresume) method.  |
| [removeAllTransferEventListeners](index.md#-812467576%2FFunctions%2F1615067946) | [androidJvm]<br>abstract fun [removeAllTransferEventListeners](index.md#-812467576%2FFunctions%2F1615067946)(): TransferEvent.Listenable                                                                                                                                                                                                                                                                                                                                           |
| [removeTransferEventListener](index.md#1314902509%2FFunctions%2F1615067946)     | [androidJvm]<br>abstract fun [removeTransferEventListener](index.md#1314902509%2FFunctions%2F1615067946)(listener: TransferEvent.Listener): TransferEvent.Listenable                                                                                                                                                                                                                                                                                                               |
| [sendResponse](send-response.md)                                                | [androidJvm]<br>abstract fun [sendResponse](send-response.md)(response: Response)<br>Send a response to verifier                                                                                                                                                                                                                                                                                                                                                                   |
| [startProximityPresentation](start-proximity-presentation.md)                   | [androidJvm]<br>abstract fun [startProximityPresentation](start-proximity-presentation.md)()<br>Starts the proximity presentation. The QR code is available through the TransferEvent.QrEngagementReady event which is triggered almost immediately after calling this method. The wallet should display the QR code to the verifier in order to start the proximity presentation.                                                                                                 |
| [startRemotePresentation](start-remote-presentation.md)                         | [androidJvm]<br>abstract fun [startRemotePresentation](start-remote-presentation.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))<br>Start a remote presentation with the given URI The URI could be either                                                                                                                                                                                                                                    |
| [stopProximityPresentation](stop-proximity-presentation.md)                     | [androidJvm]<br>abstract fun [stopProximityPresentation](stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = SEND_SESSION_TERMINATION_MESSAGE)<br>Stops the proximity presentation. Method receives flags that can be used to control the session termination. The available flags are:                                                                                                                            |
