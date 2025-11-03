//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../index.md)/[TransactionLogger](index.md)

# TransactionLogger

[androidJvm]\
fun interface [TransactionLogger](index.md)

A logger for transactions.

Implementations of this interface should log transactions to some persistent storage. The storage can be a file, a database, or any other storage medium.

## Functions

| Name | Summary |
|---|---|
| [log](log.md) | [androidJvm]<br>abstract fun [log](log.md)(transaction: [TransactionLog](../-transaction-log/index.md))<br>Logs a transaction. |
