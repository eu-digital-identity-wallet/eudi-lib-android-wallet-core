//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIManager](index.md)

# DCAPIManager

[release]\
class [DCAPIManager](index.md)(requestProcessor: RequestProcessor, var logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, var listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null) : TransferEvent.Listenable, ReaderTrustStoreAware

[DCAPIManager](index.md) is responsible for managing requests and responses for the Digital Credential API (DCAPI). Currently, it supports the protocol `org-iso-mdoc` according to the ISO/IEC TS 18013-7:2025 Annex C.

## Constructors

| | |
|---|---|
| [DCAPIManager](-d-c-a-p-i-manager.md) | [release]<br>constructor(requestProcessor: RequestProcessor, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, listenersExecutor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [listenersExecutor](listeners-executor.md) | [release]<br>var [listenersExecutor](listeners-executor.md): [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?<br>Optional executor for running listener callbacks. |
| [logger](logger.md) | [release]<br>var [logger](logger.md): [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>Optional logger for logging events. |
| [readerTrustStore](reader-trust-store.md) | [release]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore? |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [release]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [DCAPIManager](index.md) |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [release]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [DCAPIManager](index.md) |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [release]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [DCAPIManager](index.md) |
| [resolveRequest](resolve-request.md) | [release]<br>fun [resolveRequest](resolve-request.md)(request: Request) |
| [sendResponse](send-response.md) | [release]<br>fun [sendResponse](send-response.md)(response: Response) |