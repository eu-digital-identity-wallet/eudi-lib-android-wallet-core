//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpManager](index.md)

# OpenId4VpManager

[androidJvm]\
class [OpenId4VpManager](index.md)(val config: [OpenId4VpConfig](../-open-id4-vp-config/index.md),
val requestProcessor: [OpenId4VpRequestProcessor](../-open-id4-vp-request-processor/index.md), var
logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, var
listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? =
null, val ktorHttpClientFactory: () -&gt; HttpClient? = null) : TransferEvent.Listenable,
ReaderTrustStoreAware

## Constructors

|                                             |                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [OpenId4VpManager](-open-id4-vp-manager.md) | [androidJvm]<br>constructor(config: [OpenId4VpConfig](../-open-id4-vp-config/index.md), requestProcessor: [OpenId4VpRequestProcessor](../-open-id4-vp-request-processor/index.md), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, ktorHttpClientFactory: () -&gt; HttpClient? = null) |

## Types

| Name                             | Summary                                                 |
|----------------------------------|---------------------------------------------------------|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name                                                 | Summary                                                                                                                                                        |
|------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [config](config.md)                                  | [androidJvm]<br>val [config](config.md): [OpenId4VpConfig](../-open-id4-vp-config/index.md)                                                                    |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [androidJvm]<br>val [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient? = null                                                          |
| [listenersExecutor](listeners-executor.md)           | [androidJvm]<br>var [listenersExecutor](listeners-executor.md): [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? |
| [logger](logger.md)                                  | [androidJvm]<br>var [logger](logger.md): [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?                                                    |
| [readerTrustStore](reader-trust-store.md)            | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?                                                                 |
| [requestProcessor](request-processor.md)             | [androidJvm]<br>val [requestProcessor](request-processor.md): [OpenId4VpRequestProcessor](../-open-id4-vp-request-processor/index.md)                          |

## Functions

| Name                                                                      | Summary                                                                                                                                                            |
|---------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [addTransferEventListener](add-transfer-event-listener.md)                | [androidJvm]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4VpManager](index.md)       |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [OpenId4VpManager](index.md)                        |
| [removeTransferEventListener](remove-transfer-event-listener.md)          | [androidJvm]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4VpManager](index.md) |
| [resolveRequestUri](resolve-request-uri.md)                               | [androidJvm]<br>fun [resolveRequestUri](resolve-request-uri.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))             |
| [sendResponse](send-response.md)                                          | [androidJvm]<br>fun [sendResponse](send-response.md)(response: Response)                                                                                           |
