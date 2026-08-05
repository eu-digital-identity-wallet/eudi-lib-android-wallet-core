//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../../index.md)/[DeviceRequestProcessor](../index.md)/[Helper](index.md)

# Helper

[release]\
class [Helper](index.md)(documentManager: DocumentManager)

Helper class to process the [RequestedMdocDocument](../-requested-mdoc-document/index.md) and return the [RequestedDocuments](../../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md).

## Constructors

| | |
|---|---|
| [Helper](-helper.md) | [release]<br>constructor(documentManager: DocumentManager) |

## Functions

| Name | Summary |
|---|---|
| [getRequestedDocuments](get-requested-documents.md) | [release]<br>suspend fun [getRequestedDocuments](get-requested-documents.md)(requestedMdocDocuments: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRequestProcessor.RequestedMdocDocument](../-requested-mdoc-document/index.md)&gt;): [RequestedDocuments](../../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md)<br>Get the [RequestedDocuments](../../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md) from the [RequestedMdocDocument](../-requested-mdoc-document/index.md). |