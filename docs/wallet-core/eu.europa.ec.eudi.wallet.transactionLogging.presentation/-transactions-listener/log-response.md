//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)/[logResponse](log-response.md)

# logResponse

[androidJvm]\
fun [logResponse](log-response.md)(response: Response, e: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null)

Logs the response after sending it to the relying party This method is called from [TransactionsDecorator.sendResponse](../-transactions-decorator/send-response.md)

#### Parameters

androidJvm

| | |
|---|---|
| response | the response to be logged |
| e | the exception that occurred during sending the response, if any |
