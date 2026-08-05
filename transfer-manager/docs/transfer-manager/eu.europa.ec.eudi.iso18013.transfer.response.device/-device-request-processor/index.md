//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../index.md)/[DeviceRequestProcessor](index.md)

# DeviceRequestProcessor

[release]\
class [DeviceRequestProcessor](index.md)(documentManager: DocumentManager, var readerTrustStore: [ReaderTrustStore](../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store/index.md)? = null, readerAuthPolicy: [ReaderAuthPolicy](../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md) = ReaderAuthPolicy.EnforceIfPresent, zkSystemRepository: ZkSystemRepository? = null, zkResponsePolicy: [ZkResponsePolicy](../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md) = ZkResponsePolicy.FallbackToFullDisclosure) : [RequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/index.md), [ReaderTrustStoreAware](../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store-aware/index.md)

Implementation of [RequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/index.md) for [DeviceRequest](../-device-request/index.md) for the ISO 18013-5 standard.

## Constructors

| | |
|---|---|
| [DeviceRequestProcessor](-device-request-processor.md) | [release]<br>constructor(documentManager: DocumentManager, readerTrustStore: [ReaderTrustStore](../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store/index.md)? = null, readerAuthPolicy: [ReaderAuthPolicy](../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md) = ReaderAuthPolicy.EnforceIfPresent, zkSystemRepository: ZkSystemRepository? = null, zkResponsePolicy: [ZkResponsePolicy](../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md) = ZkResponsePolicy.FallbackToFullDisclosure) |

## Types

| Name | Summary |
|---|---|
| [Helper](-helper/index.md) | [release]<br>class [Helper](-helper/index.md)(documentManager: DocumentManager)<br>Helper class to process the [RequestedMdocDocument](-requested-mdoc-document/index.md) and return the [RequestedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md). |
| [RequestedMdocDocument](-requested-mdoc-document/index.md) | [release]<br>data class [RequestedMdocDocument](-requested-mdoc-document/index.md)(val docType: DocType, val requested: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;NameSpace, [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;ElementIdentifier, [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)&gt;&gt;, val readerAuthentication: () -&gt; [ReaderAuth](../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth/index.md)?, val zkRequestSystemSpecs: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ZkSystemSpec&gt;? = null)<br>Parsed requested document. |

## Properties

| Name | Summary |
|---|---|
| [readerTrustStore](reader-trust-store.md) | [release]<br>open override var [readerTrustStore](reader-trust-store.md): [ReaderTrustStore](../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store/index.md)?<br>the reader trust store to perform reader authentication |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [release]<br>open override fun [process](process.md)(request: [Request](../../eu.europa.ec.eudi.iso18013.transfer.response/-request/index.md)): [RequestProcessor.ProcessedRequest](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/index.md)<br>Process the [DeviceRequest](../-device-request/index.md) and return the [ProcessedDeviceRequest](../-processed-device-request/index.md) or a [RequestProcessor.ProcessedRequest.Failure](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/-processed-request/-failure/index.md). |