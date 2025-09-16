//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIManager](index.md)

# DCAPIManager

[androidJvm]\
class [DCAPIManager](index.md)(requestProcessor: RequestProcessor, var logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, var listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null) : TransferEvent.Listenable, ReaderTrustStoreAware

[DCAPIManager](index.md) is responsible for managing requests and responses for the Digital Credential API (DCAPI). Currently, it supports the protocol `org-iso-mdoc` according to the ISO/IEC TS 18013-7:2025 Annex C.

## Constructors

| | |
|---|---|
| [DCAPIManager](-d-c-a-p-i-manager.md) | [androidJvm]<br>constructor(requestProcessor: RequestProcessor, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [listenersExecutor](listeners-executor.md) | [androidJvm]<br>var [listenersExecutor](listeners-executor.md): [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?<br>Optional executor for running listener callbacks. |
| [logger](logger.md) | [androidJvm]<br>var [logger](logger.md): [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>Optional logger for logging events. |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore? |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): &lt;Error class: unknown class&gt; |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): &lt;Error class: unknown class&gt; |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): &lt;Error class: unknown class&gt; |
| [resolveRequest](resolve-request.md) | [androidJvm]<br>fun [resolveRequest](resolve-request.md)(request: Request) |
| [sendResponse](send-response.md) | [androidJvm]<br>fun [sendResponse](send-response.md)(response: Response) |
