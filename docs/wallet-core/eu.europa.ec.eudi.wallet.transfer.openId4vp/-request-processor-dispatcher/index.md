//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[RequestProcessorDispatcher](index.md)

# RequestProcessorDispatcher

[androidJvm]\
class [RequestProcessorDispatcher](index.md)(val presentationDefinitionProcessor: RequestProcessor, val digitalCredentialsQueryProcessor: RequestProcessor, var openId4VpReaderTrust: [OpenId4VpReaderTrust](../-open-id4-vp-reader-trust/index.md)) : RequestProcessor, ReaderTrustStoreAware

Dispatches OpenID4VP request processing to the appropriate processor based on the presentation query type.

This class acts as a router for OpenID4VP requests, delegating processing to either a Presentation Definition processor or a Digital Credentials Query processor, depending on the type of query in the resolved request object. It also manages the reader trust store for verifying reader certificates.

## Constructors

| | |
|---|---|
| [RequestProcessorDispatcher](-request-processor-dispatcher.md) | [androidJvm]<br>constructor(presentationDefinitionProcessor: RequestProcessor, digitalCredentialsQueryProcessor: RequestProcessor, openId4VpReaderTrust: [OpenId4VpReaderTrust](../-open-id4-vp-reader-trust/index.md)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [digitalCredentialsQueryProcessor](digital-credentials-query-processor.md) | [androidJvm]<br>val [digitalCredentialsQueryProcessor](digital-credentials-query-processor.md): RequestProcessor<br>Processor for Digital Credentials Query (DCQL) queries. |
| [openId4VpReaderTrust](open-id4-vp-reader-trust.md) | [androidJvm]<br>var [openId4VpReaderTrust](open-id4-vp-reader-trust.md): [OpenId4VpReaderTrust](../-open-id4-vp-reader-trust/index.md)<br>Trust anchor and store for reader certificate validation. |
| [presentationDefinitionProcessor](presentation-definition-processor.md) | [androidJvm]<br>val [presentationDefinitionProcessor](presentation-definition-processor.md): RequestProcessor<br>Processor for Presentation Definition queries. |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?<br>The trust store used for verifying reader certificates. |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [androidJvm]<br>open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest<br>Processes an [OpenId4VpRequest](../-open-id4-vp-request/index.md) by dispatching to the appropriate processor based on the query type. |
