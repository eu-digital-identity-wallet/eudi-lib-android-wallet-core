//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](../index.md)/[OpenId4vpManager](index.md)

# OpenId4vpManager

[androidJvm]\
class [OpenId4vpManager](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), verifierApi: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentManager: DocumentManager) : TransferEvent.Listenable

OpenId4vp manager. This class is used to manage the OpenId4vp. It is used to resolve the request uri and send the response.

Example:

```kotlin
val openId4vpManager = OpenId4vpManager(
   context,
   verifierApi = "https://verifier-api.com"
   documentManager = documentManager
)
val transferEventListener = TransferEvent.Listener { event ->
  when (event) {
     is TransferEvent.Connecting -> {
         // inform user
     }
     is TransferEvent.RequestReceived -> {
         val request = openId4vpManager.resolveRequestUri(event.request)
         // handle request and demand from user the documents to be disclosed
         val disclosedDocuments = listOf<DisclosedDocument>()
         val response = EudiWalletSDK.createResponse(disclosedDocuments)
         openId4vpManager.sendResponse(response)
     }
  }
}
openId4vpManager.addTransferEventListener(transferEventListener)

@property documentManager
@constructor

@param context
@param verifierApi
```

## Constructors

| | |
|---|---|
| [OpenId4vpManager](-open-id4vp-manager.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), verifierApi: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentManager: DocumentManager) |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4vpManager](index.md) |
| [close](close.md) | [androidJvm]<br>fun [close](close.md)() |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [OpenId4vpManager](index.md) |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4vpManager](index.md) |
| [resolveRequestUri](resolve-request-uri.md) | [androidJvm]<br>fun [resolveRequestUri](resolve-request-uri.md)(openid4VPURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Resolve request uri and call the listener with the request object. |
| [sendResponse](send-response.md) | [androidJvm]<br>fun [sendResponse](send-response.md)(deviceResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html))<br>Send response to the verifier. |
