//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[RequestProcessorDispatcher](index.md)/[process](process.md)

# process

[androidJvm]\
open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest

Processes an [OpenId4VpRequest](../-open-id4-vp-request/index.md) by dispatching to the appropriate processor based on the query type.

#### Return

The processed request result.

#### Parameters

androidJvm

| | |
|---|---|
| request | The request to process. Must be an [OpenId4VpRequest](../-open-id4-vp-request/index.md) with a resolved ResolvedRequestObject.OpenId4VPAuthorization. |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | if the request is not valid or supported. |
