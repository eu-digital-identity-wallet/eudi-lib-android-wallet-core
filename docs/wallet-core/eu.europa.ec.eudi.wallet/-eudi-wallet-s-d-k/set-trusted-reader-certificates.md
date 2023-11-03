//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletSDK](index.md)/[setTrustedReaderCertificates](set-trusted-reader-certificates.md)

# setTrustedReaderCertificates

[androidJvm]\
fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(trustedReaderCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [EudiWalletSDK](index.md)

Sets the readers' certificates that are trusted by the wallet

#### Return

[EudiWalletSDK](index.md)

#### Parameters

androidJvm

| | |
|---|---|
| trustedReaderCertificates | list of trusted reader certificates |

#### See also

| |
|---|
| TransferManager.setReaderTrustStore |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWalletSDK](index.md) is not firstly initialized via the [init](init.md) method |

[androidJvm]\
fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(@[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg rawRes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EudiWalletSDK](index.md)

Sets the readers' certificates from raw resources that are trusted by the wallet

#### Return

[EudiWalletSDK](index.md)

#### Parameters

androidJvm

| | |
|---|---|
| rawRes | list of raw resources of trusted reader certificates |

#### See also

| |
|---|
| TransferManager.setReaderTrustStore |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWalletSDK](index.md) is not firstly initialized via the [init](init.md) method |
