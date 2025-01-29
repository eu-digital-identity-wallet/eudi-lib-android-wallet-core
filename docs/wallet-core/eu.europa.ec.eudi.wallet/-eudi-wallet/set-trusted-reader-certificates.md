//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[setTrustedReaderCertificates](set-trusted-reader-certificates.md)

# setTrustedReaderCertificates

[androidJvm]\
abstract fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(trustedReaderCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [EudiWallet](index.md)

Sets the reader trust store with the given list of [X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html). This method is useful when the reader trust store is not set in the configuration object, or when the reader trust store needs to be updated at runtime.

#### Return

this [EudiWallet](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| readerCertificates | the list of reader certificates |

[androidJvm]\
abstract fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(@[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg rawRes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [EudiWallet](index.md)

Sets the reader trust store with the given list of raw resource IDs. This method is useful when the reader trust store is not set in the configuration object, or when the reader trust store needs to be updated at runtime.

#### Return

this [EudiWallet](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| rawRes | the list of raw resource IDs |
