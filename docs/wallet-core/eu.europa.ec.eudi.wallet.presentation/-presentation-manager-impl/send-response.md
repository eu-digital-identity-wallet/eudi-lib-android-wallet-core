//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManagerImpl](index.md)/[sendResponse](send-response.md)

# sendResponse

[androidJvm]\
open override fun [sendResponse](send-response.md)(response: Response)

Send a response to verifier

The response should be generated through the eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor.ProcessedRequest.Success.generateResponse method and sent back to the verifier by calling this method.

#### Parameters

androidJvm

| | |
|---|---|
| response | the response |
