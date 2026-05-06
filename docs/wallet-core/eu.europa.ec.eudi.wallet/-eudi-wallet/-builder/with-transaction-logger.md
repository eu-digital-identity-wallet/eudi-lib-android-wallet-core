//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withTransactionLogger](with-transaction-logger.md)

# withTransactionLogger

[release]\
fun [withTransactionLogger](with-transaction-logger.md)(transactionLogger: [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md)): [EudiWallet.Builder](index.md)

Configure with the given [TransactionLogger](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md) to use for logging transactions. If not set, the default transaction logger will be used which logs transactions to the console.

#### Return

this [Builder](index.md) instance

#### Parameters

release

| | |
|---|---|
| transactionLogger | the transaction logger |