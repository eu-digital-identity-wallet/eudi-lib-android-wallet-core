//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[ProcessedDcqlRequest](index.md)/[generateResponse](generate-response.md)

# generateResponse

[androidJvm]\
open override fun [generateResponse](generate-response.md)(disclosedDocuments: DisclosedDocuments, signatureAlgorithm: Algorithm?): ResponseResult

Generates an OpenID4VP response with verifiable presentations for the selected documents.

This method creates appropriate verifiable presentations based on the document format:

- 
   For MSO mdoc format documents, it generates ISO 18013-5 compatible presentations
- 
   For SD-JWT VC format documents, it creates SD-JWT format verifiable presentations

Important aspects:

- 
   Only one document per query ID is disclosed (if multiple are selected, only the first is used)
- 
   Each document is wrapped in its appropriate format-specific verifiable presentation
- 
   Documents are tracked with proper metadata for later transaction logging

#### Return

ResponseResult with prepared response or error information

#### Parameters

androidJvm

| | |
|---|---|
| disclosedDocuments | Documents selected by the user to disclose |
| signatureAlgorithm | Algorithm to use for signing the presentations |
