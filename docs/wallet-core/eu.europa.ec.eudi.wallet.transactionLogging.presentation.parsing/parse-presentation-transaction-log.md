//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[parsePresentationTransactionLog](parse-presentation-transaction-log.md)

# parsePresentationTransactionLog

[androidJvm]\
fun [parsePresentationTransactionLog](parse-presentation-transaction-log.md)(transactionLog: [TransactionLog](../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)): [PresentationTransactionLog](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presentation-transaction-log/index.md)

Parses a [TransactionLog](../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) and returns a [PresentationTransactionLog](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presentation-transaction-log/index.md).

#### Return

A [PresentationTransactionLog](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presentation-transaction-log/index.md) object.

#### Parameters

androidJvm

| | |
|---|---|
| transactionLog | The transaction log to parse. |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | If the transaction log is not a presentation transaction log or if any required fields are null. |
