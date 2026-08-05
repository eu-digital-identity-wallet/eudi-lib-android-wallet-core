//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../index.md)/[ProcessedDeviceRequest](index.md)/[ProcessedDeviceRequest](-processed-device-request.md)

# ProcessedDeviceRequest

[release]\
constructor(documentManager: DocumentManager, sessionTranscript: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html), requestedDocuments: [RequestedDocuments](../../eu.europa.ec.eudi.iso18013.transfer.response/-requested-documents/index.md), zkSystemRepository: ZkSystemRepository? = null, readerAuthPolicy: [ReaderAuthPolicy](../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md) = ReaderAuthPolicy.EnforceIfPresent, zkResponsePolicy: [ZkResponsePolicy](../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md) = ZkResponsePolicy.FallbackToFullDisclosure)