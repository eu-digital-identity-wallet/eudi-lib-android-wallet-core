//[transfer-manager](../../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../../../index.md)/[RequestProcessor](../../index.md)/[ProcessedRequest](../index.md)/[Success](index.md)/[generateResponse](generate-response.md)

# generateResponse

[release]\
abstract fun [generateResponse](generate-response.md)(disclosedDocuments: [DisclosedDocuments](../../../-disclosed-documents/index.md), signatureAlgorithm: Algorithm? = null): [ResponseResult](../../../-response-result/index.md)

Generates the response for the disclosed documents

#### Return

the response result containing the response or the error

#### Parameters

release

| | |
|---|---|
| disclosedDocuments | the disclosed documents |
| signatureAlgorithm | the signature algorithm |