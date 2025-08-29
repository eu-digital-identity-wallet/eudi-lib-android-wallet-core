//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpManager](index.md)

# OpenId4VpManager

[androidJvm]\
class [OpenId4VpManager](index.md)(val config: [OpenId4VpConfig](../-open-id4-vp-config/index.md), val requestProcessor: [DcqlRequestProcessor](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-dcql-request-processor/index.md), var logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, var listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, val ktorHttpClientFactory: () -&gt; HttpClient? = null) : TransferEvent.Listenable, ReaderTrustStoreAware

Manages the OpenID4VP (OpenID for Verifiable Presentations) flow in the wallet.

This class is responsible for configuring, initializing, and orchestrating the OpenID4VP protocol, including request processing, event listening, and HTTP client management. It acts as the main entry point for handling OpenID4VP requests and responses in the wallet.

## Constructors

| | |
|---|---|
| [OpenId4VpManager](-open-id4-vp-manager.md) | [androidJvm]<br>constructor(config: [OpenId4VpConfig](../-open-id4-vp-config/index.md), requestProcessor: [DcqlRequestProcessor](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-dcql-request-processor/index.md), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, ktorHttpClientFactory: () -&gt; HttpClient? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [androidJvm]<br>val [config](config.md): [OpenId4VpConfig](../-open-id4-vp-config/index.md)<br>The OpenID4VP configuration for the wallet. |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [androidJvm]<br>val [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient? = null<br>Optional factory for creating custom Ktor HTTP clients. |
| [listenersExecutor](listeners-executor.md) | [androidJvm]<br>var [listenersExecutor](listeners-executor.md): [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?<br>Optional executor for event listeners. |
| [logger](logger.md) | [androidJvm]<br>var [logger](logger.md): [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>Optional logger for diagnostic output. |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>The trust store used for verifying reader certificates. Delegates to the request processor. |
| [requestProcessor](request-processor.md) | [androidJvm]<br>val [requestProcessor](request-processor.md): [DcqlRequestProcessor](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-dcql-request-processor/index.md)<br>The dispatcher that routes requests to the appropriate processor. |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): &lt;Error class: unknown class&gt;<br>Registers a new transfer event listener. |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): &lt;Error class: unknown class&gt;<br>Removes all transfer event listeners. |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): &lt;Error class: unknown class&gt;<br>Removes a transfer event listener. |
| [resolveRequestUri](resolve-request-uri.md) | [androidJvm]<br>fun [resolveRequestUri](resolve-request-uri.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>Resolves a request URI. This method is asynchronous and the result is emitted through the TransferEvent.Listener interface. Every time it is called it cancels any previous request that is being resolved. This will lead to the TransferEvent.Disconnected event being emitted. |
| [sendResponse](send-response.md) | [androidJvm]<br>fun [sendResponse](send-response.md)(response: Response)<br>Sends a response to the verifier. This method is asynchronous and the result is emitted through the TransferEvent.Listener interface. Every time it is called it cancels any previous response that is being sent. This will lead to the TransferEvent.Disconnected event being emitted. |
| [stop](stop.md) | [androidJvm]<br>fun [stop](stop.md)()<br>Stops the manager and cancels all running connections made by the manager. When a connection is cancelled, the TransferEvent.Disconnected event is emitted. |
