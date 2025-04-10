//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../../index.md)/[PresentationTransactionLog](../index.md)/[Companion](index.md)/[fromTransactionLog](from-transaction-log.md)

# fromTransactionLog

[androidJvm]\
fun [fromTransactionLog](from-transaction-log.md)(transactionLog: [TransactionLog](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[PresentationTransactionLog](../index.md)&gt;

Parses a [TransactionLog](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) into a [PresentationTransactionLog](../index.md).

This function is a wrapper around the [parsePresentationTransactionLog](../../../eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing/parse-presentation-transaction-log.md) function.

#### Return

A [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) containing the parsed [PresentationTransactionLog](../index.md) or an exception if parsing fails.

#### Parameters

androidJvm

| | |
|---|---|
| transactionLog | The [TransactionLog](../../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) to be parsed. |
