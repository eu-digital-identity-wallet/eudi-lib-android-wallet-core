//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpRequestProcessor](index.md)/[process](process.md)

# process

[androidJvm]\
open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest

Processes an OpenID4VP request and generates an appropriate response processor.

This method determines the request type based on presentation requirements and produces a specialized processor for handling the credential disclosure flow:

1. 
   Validates the request structure and authorization format
2. 
   Extracts presentation definitions and identifies requested document formats
3. 
   For MSO_MDOC-only requests, creates a specialized processor optimized for ISO 18013-5
4. 
   For mixed format requests, creates a generic processor supporting multiple formats
5. 
   Maps requested credential fields to available documents in the wallet

#### Return

A RequestProcessor.ProcessedRequest containing matched documents and response generators or RequestProcessor.ProcessedRequest.Failure if processing fails

#### Parameters

androidJvm

| | |
|---|---|
| request | The incoming OpenID4VP request to process |
