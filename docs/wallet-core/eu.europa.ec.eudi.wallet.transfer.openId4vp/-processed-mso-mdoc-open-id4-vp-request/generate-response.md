//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[ProcessedMsoMdocOpenId4VpRequest](index.md)/[generateResponse](generate-response.md)

# generateResponse

[androidJvm]\
open override fun [generateResponse](generate-response.md)(disclosedDocuments: DisclosedDocuments, signatureAlgorithm: Algorithm?): ResponseResult

Generates an OpenID4VP response from the disclosed documents.

This method converts the disclosed documents into an OpenID4VP response by:

1. 
   Validating the OpenID4VP request object and presentation definition
2. 
   Generating a device response using the processed request
3. 
   Encoding the device response as a verifiable presentation
4. 
   Creating a presentation submission that maps input descriptors to the presentation
5. 
   Wrapping everything in an OpenID4VP response object

#### Return

A ResponseResult object containing either a successful [OpenId4VpResponse.DeviceResponse](../-open-id4-vp-response/-device-response/index.md)     or a ResponseResult.Failure with the error that occurred during processing

#### Parameters

androidJvm

| | |
|---|---|
| disclosedDocuments | Documents that the user has agreed to disclose |
| signatureAlgorithm | Optional algorithm to use for signing the response |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | if the request object is not a valid OpenId4VPAuthorization     or if the presentation query is not a PresentationDefinition |
