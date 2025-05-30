//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpRequestProcessor](index.md)

# OpenId4VpRequestProcessor

class [OpenId4VpRequestProcessor](index.md)(documentManager: DocumentManager, var readerTrustStore: ReaderTrustStore?) : RequestProcessor, ReaderTrustStoreAware

Processor for handling OpenID for Verifiable Presentation (OpenID4VP) requests.

This class implements the RequestProcessor and ReaderTrustStoreAware interfaces to process presentation requests using the OpenID4VP protocol. It supports both MSO_MDOC (Mobile Security Object Mobile Driving License) and SD-JWT VC (Selective Disclosure JWT Verifiable Credential) document formats.

The processor handles:

- 
   Parsing and validation of OpenID4VP requests
- 
   Processing of presentation definitions with different document formats
- 
   Reader authentication through X.509 certificates
- 
   Extraction of requested document claims based on input descriptors

#### Parameters

androidJvm

| | |
|---|---|
| documentManager | Manages document retrieval and processing |

## Constructors

| | |
|---|---|
| [OpenId4VpRequestProcessor](-open-id4-vp-request-processor.md) | [androidJvm]<br>constructor(documentManager: DocumentManager, readerTrustStore: ReaderTrustStore?) |

## Properties

| Name | Summary |
|---|---|
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>Provides trust information for verifying reader certificates |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [androidJvm]<br>open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest<br>Processes an OpenID4VP request and returns the appropriate processed request object. |
