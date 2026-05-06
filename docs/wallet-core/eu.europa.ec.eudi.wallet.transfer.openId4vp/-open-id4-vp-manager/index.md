//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpManager](index.md)

# OpenId4VpManager

[release]\
class [OpenId4VpManager](index.md)(val config: [OpenId4VpConfig](../-open-id4-vp-config/index.md), val requestProcessor: [DcqlRequestProcessor](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-dcql-request-processor/index.md), var logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, var listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, val ktorHttpClientFactory: () -&gt; HttpClient? = null) : TransferEvent.Listenable, ReaderTrustStoreAware

Manages the OpenID4VP (OpenID for Verifiable Presentations) flow in the wallet.

This class is responsible for configuring, initializing, and orchestrating the OpenID4VP protocol, including request processing, event listening, and HTTP client management. It acts as the main entry point for handling OpenID4VP requests and responses in the wallet.

## Constructors

| | |
|---|---|
| [OpenId4VpManager](-open-id4-vp-manager.md) | [release]<br>constructor(config: [OpenId4VpConfig](../-open-id4-vp-config/index.md), requestProcessor: [DcqlRequestProcessor](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-dcql-request-processor/index.md), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, ktorHttpClientFactory: () -&gt; HttpClient? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [release]<br>val [config](config.md): [OpenId4VpConfig](../-open-id4-vp-config/index.md)<br>The OpenID4VP configuration for the wallet. |
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [release]<br>val [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient?<br>Optional factory for creating custom Ktor HTTP clients. |
| [listenersExecutor](listeners-executor.md) | [release]<br>var [listenersExecutor](listeners-executor.md): [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?<br>Optional executor for event listeners. |
| [logger](logger.md) | [release]<br>var [logger](logger.md): [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>Optional logger for diagnostic output. |
| [readerTrustStore](reader-trust-store.md) | [release]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>The trust store used for verifying reader certificates. Delegates to the request processor. |
| [requestProcessor](request-processor.md) | [release]<br>val [requestProcessor](request-processor.md): [DcqlRequestProcessor](../../eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql/-dcql-request-processor/index.md)<br>The dispatcher that routes requests to the appropriate processor. |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [release]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4VpManager](index.md)<br>Registers a new transfer event listener. |
| [reject](reject.md) | [release]<br>fun [reject](reject.md)()<br>Called when the USER cancels-rejects the transaction from the UI. Uses the cached resolved request object to send the rejection. |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [release]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [OpenId4VpManager](index.md)<br>Removes all transfer event listeners. |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [release]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4VpManager](index.md)<br>Removes a transfer event listener. |
| [resolveRequestUri](resolve-request-uri.md) | [release]<br>fun [resolveRequestUri](resolve-request-uri.md)(uri: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html))<br>Resolves a request URI. This method is asynchronous and the result is emitted through the TransferEvent.Listener interface. Every time it is called it cancels any previous request that is being resolved. This will lead to the TransferEvent.Disconnected event being emitted. |
| [sendResponse](send-response.md) | [release]<br>fun [sendResponse](send-response.md)(response: Response)<br>Sends a response to the verifier. This method is asynchronous and the result is emitted through the TransferEvent.Listener interface. Every time it is called it cancels any previous response that is being sent. This will lead to the TransferEvent.Disconnected event being emitted. |
| [stop](stop.md) | [release]<br>fun [stop](stop.md)()<br>Stops the manager and cancels all running connections made by the manager. When a connection is cancelled, the TransferEvent.Disconnected event is emitted. |