//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)

# TransactionsListener

[androidJvm]\
class [TransactionsListener](index.md)(transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) : TransferEvent.Listener

Listener for transaction logging

Observes transfer events and logs transaction details using TransactionLogBuilder

## Constructors

| | |
|---|---|
| [TransactionsListener](-transactions-listener.md) | [androidJvm]<br>constructor(transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) |

## Functions

| Name | Summary |
|---|---|
| [logResponse](log-response.md) | [androidJvm]<br>fun [logResponse](log-response.md)(response: Response, error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null)<br>Logs the response after sending it to the relying party |
| [onTransferEvent](on-transfer-event.md) | [androidJvm]<br>open override fun [onTransferEvent](on-transfer-event.md)(event: TransferEvent)<br>Handle transfer events |
| [stop](stop.md) | [androidJvm]<br>fun [stop](stop.md)() |
