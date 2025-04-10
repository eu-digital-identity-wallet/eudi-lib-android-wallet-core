//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpRequestProcessor](index.md)

# OpenId4VpRequestProcessor

[androidJvm]\
class [OpenId4VpRequestProcessor](index.md)(documentManager: DocumentManager, var readerTrustStore: ReaderTrustStore?) : RequestProcessor, ReaderTrustStoreAware

## Constructors

| | |
|---|---|
| [OpenId4VpRequestProcessor](-open-id4-vp-request-processor.md) | [androidJvm]<br>constructor(documentManager: DocumentManager, readerTrustStore: ReaderTrustStore?) |

## Properties

| Name | Summary |
|---|---|
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore? |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [androidJvm]<br>open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest |
