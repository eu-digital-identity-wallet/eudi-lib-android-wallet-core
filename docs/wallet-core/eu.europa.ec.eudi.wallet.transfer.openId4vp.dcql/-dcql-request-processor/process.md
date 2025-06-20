//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[DcqlRequestProcessor](index.md)/[process](process.md)

# process

[androidJvm]\
open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest

Processes an OpenID4VP request containing DCQL queries.

This method performs the following steps:

1. 
   Validates the request is a properly formatted OpenID4VP request with DCQL
2. 
   Extracts reader authentication information
3. 
   Processes each credential request in the query
4. 
   Identifies matching documents in the wallet for each request
5. 
   Maps requested claims to the document items

#### Return

[ProcessedDcqlRequest](../-processed-dcql-request/index.md) containing matched documents and requested items

#### Parameters

androidJvm

| | |
|---|---|
| request | The incoming presentation request |
