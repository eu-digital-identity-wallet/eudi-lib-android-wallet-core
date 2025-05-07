//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)

# TransactionsListener

class [TransactionsListener](index.md)(transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) : TransferEvent.Listener

Listener for transaction logging.

Observes transfer events and logs transaction details using [TransactionLogBuilder](../-transaction-log-builder/index.md). It also handles logging when a presentation is stopped or an error occurs.

#### Parameters

androidJvm

| | |
|---|---|
| documentManager | The manager responsible for accessing document details. |

## Constructors

| | |
|---|---|
| [TransactionsListener](-transactions-listener.md) | [androidJvm]<br>constructor(transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) |

## Functions

| Name | Summary |
|---|---|
| [logResponse](log-response.md) | [androidJvm]<br>fun [logResponse](log-response.md)(response: Response, error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null)<br>Logs the response after sending it to the relying party. |
| [logStopped](log-stopped.md) | [androidJvm]<br>fun [logStopped](log-stopped.md)()<br>Logs that the current transaction has been stopped. |
| [onTransferEvent](on-transfer-event.md) | [androidJvm]<br>open override fun [onTransferEvent](on-transfer-event.md)(event: TransferEvent)<br>Handles transfer events to update the transaction log. |
