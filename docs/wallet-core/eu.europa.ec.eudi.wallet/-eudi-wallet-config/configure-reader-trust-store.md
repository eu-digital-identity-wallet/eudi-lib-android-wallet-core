//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureReaderTrustStore](configure-reader-trust-store.md)

# configureReaderTrustStore

[release]\
fun [configureReaderTrustStore](configure-reader-trust-store.md)(readerTrustedCertificates: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [EudiWalletConfig](index.md)

fun [configureReaderTrustStore](configure-reader-trust-store.md)(vararg readerTrustedCertificates: [X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)): [EudiWalletConfig](index.md)

Configure the built-in ReaderTrustStore. This allows to set the reader trusted certificates for the reader trust store.

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

release

| | |
|---|---|
| readerTrustedCertificates | the reader trusted certificates |

[release]\
fun [configureReaderTrustStore](configure-reader-trust-store.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), @[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg certificateRes: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html)): [EudiWalletConfig](index.md)

Configure the built-in ReaderTrustStore. This allows to set the reader trusted certificates for the reader trust store. The certificates are loaded from the raw resources.

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

release

| | |
|---|---|
| context | the context |
| certificateRes | the reader trusted certificates raw resources |