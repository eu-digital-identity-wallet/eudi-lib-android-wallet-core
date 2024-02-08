//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[sendResponse](send-response.md)

# sendResponse

[androidJvm]\
fun [sendResponse](send-response.md)(disclosedDocuments: DisclosedDocuments): ResponseResult

Send a response by giving DisclosedDocuments, i.e. the list of documents to be disclosed. The method returns a `ResponseResult` object, which can be one of the following:

1. 
   `ResponseResult.Failure`: The response creation failed. The error can be retrieved from `responseResult.error`.
2. 
   `ResponseResult.Success`: The response was created successfully. The response can be retrieved from `responseResult.response`.
3. 
   `ResponseResult.UserAuthRequired`: The response creation requires user authentication.

#### Return

ResponseResult the result of the response

#### Parameters

androidJvm

| | |
|---|---|
| disclosedDocuments | the list of documents to be disclosed in the response. |
