//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](../index.md)/[OpenId4vpManager](index.md)

# OpenId4vpManager

[androidJvm]\
class [OpenId4vpManager](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), openId4VpConfig: [OpenId4VpConfig](../-open-id4-vp-config/index.md), val responseGenerator: [OpenId4VpCBORResponseGeneratorImpl](../-open-id4-vp-c-b-o-r-response-generator-impl/index.md)) : TransferEvent.Listenable

## Constructors

| | |
|---|---|
| [OpenId4vpManager](-open-id4vp-manager.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), openId4VpConfig: [OpenId4VpConfig](../-open-id4-vp-config/index.md), responseGenerator: [OpenId4VpCBORResponseGeneratorImpl](../-open-id4-vp-c-b-o-r-response-generator-impl/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4vpManager](index.md) |
| [close](close.md) | [androidJvm]<br>fun [close](close.md)()<br>Closes the OpenId4VpManager |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [OpenId4vpManager](index.md) |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4vpManager](index.md) |
| [resolveRequestUri](resolve-request-uri.md) | [androidJvm]<br>fun [resolveRequestUri](resolve-request-uri.md)(openid4VPURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Resolve a request uri |
| [sendResponse](send-response.md) | [androidJvm]<br>fun [sendResponse](send-response.md)(deviceResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html))<br>Sends a response to the verifier |
| [setExecutor](set-executor.md) | [androidJvm]<br>fun [setExecutor](set-executor.md)(executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html))<br>Setting the `executor` is optional and defines the executor that will be used to execute the callback. If the `executor` is not defined, the callback will be executed on the main thread. |

## Properties

| Name | Summary |
|---|---|
| [responseGenerator](response-generator.md) | [androidJvm]<br>val [responseGenerator](response-generator.md): [OpenId4VpCBORResponseGeneratorImpl](../-open-id4-vp-c-b-o-r-response-generator-impl/index.md) |
