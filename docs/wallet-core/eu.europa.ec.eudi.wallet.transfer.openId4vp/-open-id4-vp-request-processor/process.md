//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpRequestProcessor](index.md)/[process](process.md)

# process

[androidJvm]\
open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest

Processes an OpenID4VP request and returns the appropriate processed request object.

This method validates that the incoming request is an [OpenId4VpRequest](../-open-id4-vp-request/index.md) with a valid presentation definition, then processes it based on the requested document formats:

- 
   For MSO_MDOC-only requests, returns a [ProcessedMsoMdocOpenId4VpRequest](../-processed-mso-mdoc-open-id4-vp-request/index.md)
- 
   For mixed format requests, returns a [ProcessedGenericOpenId4VpRequest](../-processed-generic-open-id4-vp-request/index.md) supporting both MSO_MDOC and SD-JWT VC formats

#### Return

A RequestProcessor.ProcessedRequest object containing the processed data

#### Parameters

androidJvm

| | |
|---|---|
| request | The request to process, must be an [OpenId4VpRequest](../-open-id4-vp-request/index.md) instance |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | if the request is not an [OpenId4VpRequest](../-open-id4-vp-request/index.md), lacks an OpenId4VPAuthorization,     uses an unsupported presentation format, or has no requested formats |
