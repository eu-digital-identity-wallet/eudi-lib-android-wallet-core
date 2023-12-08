//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWalletConfig](../index.md)/[Builder](index.md)/[trustedReaderCertificates](trusted-reader-certificates.md)

# trustedReaderCertificates

[androidJvm]\
fun [trustedReaderCertificates](trusted-reader-certificates.md)(
trustedReaderCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)
&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)
&gt;): [EudiWalletConfig.Builder](index.md)

Trusted reader certificates. This is the list of trusted reader certificates.

#### Return

[EudiWalletConfig.Builder](index.md)

#### Parameters

androidJvm

|                           |
|---------------------------|
| trustedReaderCertificates |

[androidJvm]\
fun [trustedReaderCertificates](trusted-reader-certificates.md)(@[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)
vararg
rawIds: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EudiWalletConfig.Builder](index.md)

Trusted reader certificates This is the list of trusted reader certificates as raw resource ids.

#### Return

[EudiWalletConfig.Builder](index.md)

#### Parameters

androidJvm

|        |                                      |
|--------|--------------------------------------|
| rawIds | raw resource ids of the certificates |
