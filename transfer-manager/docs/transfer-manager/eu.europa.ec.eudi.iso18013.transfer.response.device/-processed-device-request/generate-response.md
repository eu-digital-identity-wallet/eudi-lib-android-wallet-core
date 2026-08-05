//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../index.md)/[ProcessedDeviceRequest](index.md)/[generateResponse](generate-response.md)

# generateResponse

[release]\
open override fun [generateResponse](generate-response.md)(disclosedDocuments: [DisclosedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-disclosed-documents/index.md), signatureAlgorithm: Algorithm? = null): [ResponseResult](../../eu.europa.ec.eudi.iso18013.transfer.response/-response-result/index.md)

Generate the response for the disclosed documents.

#### Return

the response result with the device response or the error

#### Parameters

release

| | |
|---|---|
| disclosedDocuments | the disclosed documents |
| signatureAlgorithm | the signature algorithm to use for the document responses |