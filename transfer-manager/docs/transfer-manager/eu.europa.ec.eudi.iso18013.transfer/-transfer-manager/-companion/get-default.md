//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../../index.md)/[TransferManager](../index.md)/[Companion](index.md)/[getDefault](get-default.md)

# getDefault

[release]\

@[JvmStatic](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [getDefault](get-default.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, readerTrustStore: [ReaderTrustStore](../../../eu.europa.ec.eudi.iso18013.transfer.readerauth/-reader-trust-store/index.md)? = null, readerAuthPolicy: [ReaderAuthPolicy](../../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth-policy/index.md) = ReaderAuthPolicy.EnforceIfPresent, retrievalMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;? = null, zkSystemRepository: ZkSystemRepository? = null, zkResponsePolicy: [ZkResponsePolicy](../../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/index.md) = ZkResponsePolicy.FallbackToFullDisclosure): [TransferManager](../index.md)

Create a new instance of [TransferManager](../index.md) for the ISO 18013-5 standard.

#### Return

a [TransferManagerImpl](../../-transfer-manager-impl/index.md)

#### Parameters

release

| |
|---|
| context |
| documentManager |
| readerTrustStore |
| readerAuthPolicy |
| retrievalMethods |
| zkSystemRepository |
| zkResponsePolicy | the ZK response policy to use when ZK proof generation fails. Defaults to [ZkResponsePolicy.FallbackToFullDisclosure](../../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/-fallback-to-full-disclosure/index.md) for backwards compatibility. Consider using [ZkResponsePolicy.Strict](../../../eu.europa.ec.eudi.iso18013.transfer.zkp/-zk-response-policy/-strict/index.md) for production to prevent unintended full disclosure. |