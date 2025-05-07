//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)/[sendResponse](send-response.md)

# sendResponse

[androidJvm]\
open override fun [sendResponse](send-response.md)(response: Response)

Sends a response using the delegate [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and logs the outcome.

It attempts to send the response and then logs the response details. If sending the response is successful, it logs the response. If an error occurs while sending, it logs the response along with the error and then re-throws the original exception.

#### Parameters

androidJvm

| | |
|---|---|
| response | The response to be sent. |

#### Throws

| | |
|---|---|
| [Exception](https://developer.android.com/reference/kotlin/java/lang/Exception.html) | if an error occurs while sending the response via the delegate. |
