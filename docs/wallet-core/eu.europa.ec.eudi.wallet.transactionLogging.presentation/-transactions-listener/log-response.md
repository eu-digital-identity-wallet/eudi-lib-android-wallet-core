//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)/[logResponse](log-response.md)

# logResponse

[androidJvm]\
fun [logResponse](log-response.md)(response: Response, error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null)

Logs the response after sending it to the relying party.

This method updates the current transaction log with the response details and any error that might have occurred during the sending process. If an error occurs during logging itself, it logs that error and marks the transaction log with an error status.

#### Parameters

androidJvm

| | |
|---|---|
| response | The response to be logged. |
| error | The exception that occurred during sending the response, if any. |
