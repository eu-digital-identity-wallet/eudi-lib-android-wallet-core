//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionLogBuilder](index.md)/[withError](with-error.md)

# withError

[androidJvm]\
fun [withError](with-error.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)

Updates the provided [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) to indicate an error occurred.

Sets the log status to [TransactionLog.Status.Error](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/-error/index.md) and updates the timestamp.

#### Return

An updated [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) instance with error status.

#### Parameters

androidJvm

| | |
|---|---|
| log | The current transaction log to update. |
