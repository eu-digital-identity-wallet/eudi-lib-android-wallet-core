//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletImpl](index.md)/[setTrustedReaderCertificates](set-trusted-reader-certificates.md)

# setTrustedReaderCertificates

[androidJvm]\
open override fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(trustedReaderCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): &lt;Error class: unknown class&gt;

Sets the reader trust store with the given list of [X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html). This method is useful when the reader trust store is not set in the configuration object, or when the reader trust store needs to be updated at runtime.

#### Return

this [EudiWallet](../-eudi-wallet/index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| readerCertificates | the list of reader certificates |

[androidJvm]\
open override fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(vararg rawRes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): &lt;Error class: unknown class&gt;

Sets the reader trust store with the given list of raw resource IDs. This method is useful when the reader trust store is not set in the configuration object, or when the reader trust store needs to be updated at runtime.

#### Return

this [EudiWallet](../-eudi-wallet/index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| rawRes | the list of raw resource IDs |
