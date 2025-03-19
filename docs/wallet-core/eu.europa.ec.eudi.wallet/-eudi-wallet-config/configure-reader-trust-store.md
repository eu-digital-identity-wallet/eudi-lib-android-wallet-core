//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureReaderTrustStore](configure-reader-trust-store.md)

# configureReaderTrustStore

[androidJvm]\
fun [configureReaderTrustStore](configure-reader-trust-store.md)(readerTrustedCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): &lt;Error class: unknown class&gt;

fun [configureReaderTrustStore](configure-reader-trust-store.md)(vararg readerTrustedCertificates: [X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)): &lt;Error class: unknown class&gt;

Configure the built-in ReaderTrustStore. This allows to set the reader trusted certificates for the reader trust store.

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| readerTrustedCertificates | the reader trusted certificates |

[androidJvm]\
fun [configureReaderTrustStore](configure-reader-trust-store.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), @[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg certificateRes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): &lt;Error class: unknown class&gt;

Configure the built-in ReaderTrustStore. This allows to set the reader trusted certificates for the reader trust store. The certificates are loaded from the raw resources.

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| context | the context |
| certificateRes | the reader trusted certificates raw resources |
