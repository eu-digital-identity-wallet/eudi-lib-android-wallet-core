//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)/[TransactionsDecorator](-transactions-decorator.md)

# TransactionsDecorator

[androidJvm]\
constructor(delegate: [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md), documentManager: DocumentManager, transactionLogger: [TransactionLogger](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-logger/index.md), logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null)

#### Parameters

androidJvm

| | |
|---|---|
| documentManager | The manager for accessing document details, passed to the [TransactionsListener](../-transactions-listener/index.md). |
| transactionLogger | The logger for persisting transaction logs, passed to the [TransactionsListener](../-transactions-listener/index.md). |
| logger | Optional logger for internal logging of the decorator and listener. |
