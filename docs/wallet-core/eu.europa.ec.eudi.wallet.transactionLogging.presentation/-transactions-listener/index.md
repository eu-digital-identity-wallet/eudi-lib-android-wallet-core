//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)

# TransactionsListener

[androidJvm]\
class [TransactionsListener](index.md)(transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) : TransferEvent.Listener

Listener for transaction logging

## Constructors

| | |
|---|---|
| [TransactionsListener](-transactions-listener.md) | [androidJvm]<br>constructor(transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) |

## Functions

| Name | Summary |
|---|---|
| [logResponse](log-response.md) | [androidJvm]<br>fun [logResponse](log-response.md)(response: Response, e: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null)<br>Logs the response after sending it to the relying party This method is called from [TransactionsDecorator.sendResponse](../-transactions-decorator/send-response.md) |
| [onTransferEvent](on-transfer-event.md) | [androidJvm]<br>open override fun [onTransferEvent](on-transfer-event.md)(event: TransferEvent)<br>Listen for transfer events |
