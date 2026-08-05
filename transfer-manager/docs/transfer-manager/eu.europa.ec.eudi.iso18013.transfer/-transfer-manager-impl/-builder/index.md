//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../../index.md)/[TransferManagerImpl](../index.md)/[Builder](index.md)

# Builder

class [Builder](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))

Builder class for instantiating a [TransferManager](../../-transfer-manager/index.md) implementation

#### Parameters

release

| |
|---|
| context |

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) |

## Properties

| Name | Summary |
|---|---|
| [documentManager](document-manager.md) | [release]<br>var [documentManager](document-manager.md): DocumentManager?<br>document manager instance |
| [readerAuthPolicy](reader-auth-policy.md) | [release]<br>var [readerAuthPolicy](reader-auth-policy.md): [ReaderAuthPolicy](../../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md) |
| [readerTrustStore](reader-trust-store.md) | [release]<br>var [readerTrustStore](reader-trust-store.md): [ReaderTrustStore](../../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store/index.md)?<br>reader trust store instance |
| [retrievalMethods](retrieval-methods.md) | [release]<br>var [retrievalMethods](retrieval-methods.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;?<br>list of device retrieval methods |
| [zkResponsePolicy](zk-response-policy.md) | [release]<br>var [zkResponsePolicy](zk-response-policy.md): [ZkResponsePolicy](../../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md)<br>ZK response policy |
| [zkSystemRepository](zk-system-repository.md) | [release]<br>var [zkSystemRepository](zk-system-repository.md): ZkSystemRepository?<br>ZK system repository instance |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [TransferManagerImpl](../index.md)<br>Build a [eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl](../index.md) instance with [DeviceRequestProcessor](../../../eu.europa.ec.eudi.iso18013.transfer.response.device/-device-request-processor/index.md) instance |
| [documentManager](document-manager.md) | [release]<br>fun [documentManager](document-manager.md)(documentManager: DocumentManager): [TransferManagerImpl.Builder](index.md)<br>Document manager instance that will be used to retrieve the requested documents |
| [readerAuthPolicy](reader-auth-policy.md) | [release]<br>fun [readerAuthPolicy](reader-auth-policy.md)(readerAuthPolicy: [ReaderAuthPolicy](../../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md)): [TransferManagerImpl.Builder](index.md)<br>Policy for enforcing reader authentication results during response generation. Default is [ReaderAuthPolicy.EnforceIfPresent](../../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/-enforce-if-present/index.md). |
| [readerTrustStore](reader-trust-store.md) | [release]<br>fun [readerTrustStore](reader-trust-store.md)(readerTrustStore: [ReaderTrustStore](../../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store/index.md)): [TransferManagerImpl.Builder](index.md)<br>Reader trust store instance that will be used to verify the reader's certificate |
| [retrievalMethods](retrieval-methods.md) | [release]<br>fun [retrievalMethods](retrieval-methods.md)(retrievalMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;): [TransferManagerImpl.Builder](index.md)<br>Retrieval methods that will be used to retrieve the device request from the mdoc verifier |
| [zkResponsePolicy](zk-response-policy.md) | [release]<br>fun [zkResponsePolicy](zk-response-policy.md)(zkResponsePolicy: [ZkResponsePolicy](../../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md)): [TransferManagerImpl.Builder](index.md)<br>ZK response policy that determines behavior when ZK proof generation fails. Defaults to [ZkResponsePolicy.FallbackToFullDisclosure](../../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/-fallback-to-full-disclosure/index.md) for backwards compatibility. Consider using [ZkResponsePolicy.Strict](../../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/-strict/index.md) for production to prevent unintended full disclosure. |
| [zkSystemRepository](zk-system-repository.md) | [release]<br>fun [zkSystemRepository](zk-system-repository.md)(zkSystemRepository: ZkSystemRepository): [TransferManagerImpl.Builder](index.md)<br>ZK system repository that holds the zero-knowledge proof systems |