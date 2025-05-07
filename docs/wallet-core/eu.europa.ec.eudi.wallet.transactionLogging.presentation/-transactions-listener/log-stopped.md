//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)/[logStopped](log-stopped.md)

# logStopped

[androidJvm]\
fun [logStopped](log-stopped.md)()

Logs that the current transaction has been stopped.

If the current transaction log is incomplete (i.e., its status is [TransactionLog.Status.Incomplete](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/-incomplete/index.md)), this method marks the log with an [TransactionLog.Status.Error](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/-error/index.md) status and persists it. This is typically called when a presentation is explicitly stopped or disconnected before completion.
