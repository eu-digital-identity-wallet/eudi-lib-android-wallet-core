//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](../index.md)/[OpenId4VpCBORResponseGeneratorImpl](index.md)

# OpenId4VpCBORResponseGeneratorImpl

class [OpenId4VpCBORResponseGeneratorImpl](index.md)(documentsResolver: DocumentsResolver, storageEngine: StorageEngine,
secureArea: AndroidKeystoreSecureArea, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? =
null) : ResponseGenerator&lt;[OpenId4VpRequest](../-open-id4-vp-request/index.md)&gt;

OpenId4VpCBORResponseGeneratorImpl class is used for parsing a request (Presentation Definition) and generating the DeviceResponse

#### Parameters

androidJvm

| | |
|---|---|
| documentsResolver | document manager instance |
| storageEngine | storage engine used to store documents |
| secureArea | secure area used to store documents' keys |

## Constructors

|                                                                                       |                                                                                                                                                                                                                           |
|---------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [OpenId4VpCBORResponseGeneratorImpl](-open-id4-vp-c-b-o-r-response-generator-impl.md) | [androidJvm]<br>constructor(documentsResolver: DocumentsResolver, storageEngine: StorageEngine, secureArea: AndroidKeystoreSecureArea, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null) |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) |

## Functions

| Name | Summary |
|---|---|
| [createResponse](create-response.md) | [androidJvm]<br>open override fun [createResponse](create-response.md)(disclosedDocuments: DisclosedDocuments): ResponseResult<br>Creates a response and returns a ResponseResult |
| [parseRequest](parse-request.md) | [androidJvm]<br>open override fun [parseRequest](parse-request.md)(request: [OpenId4VpRequest](../-open-id4-vp-request/index.md)): RequestedDocumentData<br>Parses a request and returns the requested document data |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>fun [readerTrustStore](reader-trust-store.md)(readerTrustStore: ReaderTrustStore): [OpenId4VpCBORResponseGeneratorImpl](index.md)<br>Set a trust store so that reader authentication can be performed. |
| [setReaderTrustStore](set-reader-trust-store.md) | [androidJvm]<br>open override fun [setReaderTrustStore](set-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): [OpenId4VpCBORResponseGeneratorImpl](index.md)<br>Set a trust store so that reader authentication can be performed. |
