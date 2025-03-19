//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManagerImpl](index.md)

# PresentationManagerImpl

[androidJvm]\
class [PresentationManagerImpl](index.md)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(transferManager: TransferManager, openId4vpManager: [OpenId4VpManager](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md)? = null, val nfcEngagementServiceClass: [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? = null) : [PresentationManager](../-presentation-manager/index.md)

Implementation of the [PresentationManager](../-presentation-manager/index.md) interface based on the TransferManager and [OpenId4VpManager](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) implementations.

## Constructors

| | |
|---|---|
| [PresentationManagerImpl](-presentation-manager-impl.md) | [androidJvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>constructor(transferManager: TransferManager, openId4vpManager: [OpenId4VpManager](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md)? = null, nfcEngagementServiceClass: [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [nfcEngagementServiceClass](nfc-engagement-service-class.md) | [androidJvm]<br>open override val [nfcEngagementServiceClass](nfc-engagement-service-class.md): [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? = null<br>the NFC engagement service class |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>the reader trust store |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): &lt;Error class: unknown class&gt; |
| [disableNFCEngagement](disable-n-f-c-engagement.md) | [androidJvm]<br>open override fun [disableNFCEngagement](disable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): &lt;Error class: unknown class&gt;<br>Disable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onPause](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html#onpause) method. |
| [enableNFCEngagement](enable-n-f-c-engagement.md) | [androidJvm]<br>open override fun [enableNFCEngagement](enable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): &lt;Error class: unknown class&gt;<br>Enable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onResume](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html#onresume) method. |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): &lt;Error class: unknown class&gt; |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): &lt;Error class: unknown class&gt; |
| [sendResponse](send-response.md) | [androidJvm]<br>open override fun [sendResponse](send-response.md)(response: Response)<br>Send a response to verifier |
| [startProximityPresentation](start-proximity-presentation.md) | [androidJvm]<br>open override fun [startProximityPresentation](start-proximity-presentation.md)()<br>Starts the proximity presentation. The QR code is available through the TransferEvent.QrEngagementReady event which is triggered almost immediately after calling this method. The wallet should display the QR code to the verifier in order to start the proximity presentation. |
| [startRemotePresentation](start-remote-presentation.md) | [androidJvm]<br>open override fun [startRemotePresentation](start-remote-presentation.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Start a remote presentation with the given intent The intent.data could either contain the URI of<br>[androidJvm]<br>open override fun [startRemotePresentation](start-remote-presentation.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html), refererUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?)<br>Start a remote presentation with the given URI The URI could be either |
| [stopProximityPresentation](stop-proximity-presentation.md) | [androidJvm]<br>open override fun [stopProximityPresentation](stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Stops the proximity presentation. Method receives flags that can be used to control the session termination. The available flags are: |
| [stopRemotePresentation](stop-remote-presentation.md) | [androidJvm]<br>open override fun [stopRemotePresentation](stop-remote-presentation.md)()<br>Stops any ongoing remote presentation |
