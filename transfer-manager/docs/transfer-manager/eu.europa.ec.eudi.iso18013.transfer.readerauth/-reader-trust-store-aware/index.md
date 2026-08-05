//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../index.md)/[ReaderTrustStoreAware](index.md)

# ReaderTrustStoreAware

interface [ReaderTrustStoreAware](index.md)

Interface that indicates that implementation are aware of a [ReaderTrustStore](../-reader-trust-store/index.md) that can be used to verify the authenticity of a reader.

#### See also

| |
|---|
| [ReaderTrustStore](../-reader-trust-store/index.md) |

#### Inheritors

| |
|---|
| [TransferManagerImpl](../../eu.europa.ec.eudi.iso18013.transfer/-transfer-manager-impl/index.md) |
| [DeviceRequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response.device/-device-request-processor/index.md) |

## Properties

| Name | Summary |
|---|---|
| [readerTrustStore](reader-trust-store.md) | [release]<br>abstract var [readerTrustStore](reader-trust-store.md): [ReaderTrustStore](../-reader-trust-store/index.md)?<br>the reader trust store |