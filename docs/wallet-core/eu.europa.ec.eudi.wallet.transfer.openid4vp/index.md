//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [ClientId](index.md#-875823108%2FClasslikes%2F1615067946) | [androidJvm]<br>typealias [ClientId](index.md#-875823108%2FClasslikes%2F1615067946) = [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ClientIdScheme](-client-id-scheme/index.md) | [androidJvm]<br>interface [ClientIdScheme](-client-id-scheme/index.md) |
| [DeviceResponseBytes](index.md#943895756%2FClasslikes%2F1615067946) | [androidJvm]<br>typealias [DeviceResponseBytes](index.md#943895756%2FClasslikes%2F1615067946) = [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html) |
| [EncryptionAlgorithm](-encryption-algorithm/index.md) | [androidJvm]<br>interface [EncryptionAlgorithm](-encryption-algorithm/index.md) |
| [EncryptionMethod](-encryption-method/index.md) | [androidJvm]<br>interface [EncryptionMethod](-encryption-method/index.md) |
| [OpenId4VpCBORResponse](-open-id4-vp-c-b-o-r-response/index.md) | [androidJvm]<br>class [OpenId4VpCBORResponse](-open-id4-vp-c-b-o-r-response/index.md)(val deviceResponseBytes: [DeviceResponseBytes](index.md#943895756%2FClasslikes%2F1615067946)) : Response |
| [OpenId4VpCBORResponseGeneratorImpl](-open-id4-vp-c-b-o-r-response-generator-impl/index.md) | [androidJvm]<br>class [OpenId4VpCBORResponseGeneratorImpl](-open-id4-vp-c-b-o-r-response-generator-impl/index.md)(documentsResolver: DocumentsResolver, storageEngine: StorageEngine, secureArea: AndroidKeystoreSecureArea) : ResponseGenerator&lt;[OpenId4VpRequest](-open-id4-vp-request/index.md)&gt; <br>OpenId4VpCBORResponseGeneratorImpl class is used for parsing a request (Presentation Definition) and generating the DeviceResponse |
| [OpenId4VpConfig](-open-id4-vp-config/index.md) | [androidJvm]<br>class [OpenId4VpConfig](-open-id4-vp-config/index.md)<br>Configuration for the OpenId4Vp transfer. |
| [OpenId4vpManager](-open-id4vp-manager/index.md) | [androidJvm]<br>class [OpenId4vpManager](-open-id4vp-manager/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), openId4VpConfig: [OpenId4VpConfig](-open-id4-vp-config/index.md), val responseGenerator: [OpenId4VpCBORResponseGeneratorImpl](-open-id4-vp-c-b-o-r-response-generator-impl/index.md)) : TransferEvent.Listenable |
| [OpenId4VpRequest](-open-id4-vp-request/index.md) | [androidJvm]<br>class [OpenId4VpRequest](-open-id4-vp-request/index.md)(val presentationDefinition: PresentationDefinition) : Request |
| [PreregisteredVerifier](-preregistered-verifier/index.md) | [androidJvm]<br>data class [PreregisteredVerifier](-preregistered-verifier/index.md)(var clientId: [ClientId](index.md#-875823108%2FClasslikes%2F1615067946), var verifierApi: [VerifierApi](index.md#-1538977700%2FClasslikes%2F1615067946)) |
| [VerifierApi](index.md#-1538977700%2FClasslikes%2F1615067946) | [androidJvm]<br>typealias [VerifierApi](index.md#-1538977700%2FClasslikes%2F1615067946) = [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
