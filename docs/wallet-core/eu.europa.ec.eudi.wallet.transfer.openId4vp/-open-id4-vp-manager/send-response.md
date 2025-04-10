//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpManager](index.md)/[sendResponse](send-response.md)

# sendResponse

[androidJvm]\
fun [sendResponse](send-response.md)(response: Response)

Sends a response to the verifier. This method is asynchronous and the result is emitted through the TransferEvent.Listener interface. Every time it is called it cancels any previous response that is being sent. This will lead to the TransferEvent.Disconnected event being emitted.

#### Parameters

androidJvm

| | |
|---|---|
| response | The response to send. |

#### See also

| |
|---|
| [OpenId4VpManager.addTransferEventListener](add-transfer-event-listener.md) |
