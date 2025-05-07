//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionLogBuilder](index.md)/[createEmptyPresentationLog](create-empty-presentation-log.md)

# createEmptyPresentationLog

[androidJvm]\
fun [createEmptyPresentationLog](create-empty-presentation-log.md)(): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)

Creates an initial, empty [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) for a presentation.

The created log has a status of [TransactionLog.Status.Incomplete](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/-incomplete/index.md) and type [TransactionLog.Type.Presentation](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-type/-presentation/index.md).

#### Return

A new [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) instance.
