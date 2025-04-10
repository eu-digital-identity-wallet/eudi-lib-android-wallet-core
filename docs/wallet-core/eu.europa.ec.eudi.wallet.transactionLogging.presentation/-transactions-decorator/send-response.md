//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)/[sendResponse](send-response.md)

# sendResponse

[androidJvm]\
open override fun [sendResponse](send-response.md)(response: Response)

Sends a response and logs it.

This method is responsible for sending the response through the [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and logging the response.

#### Parameters

androidJvm

| | |
|---|---|
| response | the response to be sent |

#### Throws

| | |
|---|---|
| [Exception](https://developer.android.com/reference/kotlin/java/lang/Exception.html) | if an error occurs while sending the response |
