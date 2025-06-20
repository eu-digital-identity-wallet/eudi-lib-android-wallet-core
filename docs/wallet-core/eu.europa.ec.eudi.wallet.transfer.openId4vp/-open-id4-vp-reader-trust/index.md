//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpReaderTrust](index.md)

# OpenId4VpReaderTrust

interface [OpenId4VpReaderTrust](index.md) : X509CertificateTrust

#### Inheritors

| |
|---|
| [OpenId4VpReaderTrustImpl](../-open-id4-vp-reader-trust-impl/index.md) |

## Properties

| Name | Summary |
|---|---|
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>abstract var [readerTrustStore](reader-trust-store.md): ReaderTrustStore? |
| [result](result.md) | [androidJvm]<br>abstract val [result](result.md): [ReaderTrustResult](../-reader-trust-result/index.md) |

## Functions

| Name | Summary |
|---|---|
| [isTrusted](index.md#-162011122%2FFunctions%2F1615067946) | [androidJvm]<br>abstract fun [isTrusted](index.md#-162011122%2FFunctions%2F1615067946)(chain: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
