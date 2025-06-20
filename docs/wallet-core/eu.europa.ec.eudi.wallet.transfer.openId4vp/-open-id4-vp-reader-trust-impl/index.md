//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpReaderTrustImpl](index.md)

# OpenId4VpReaderTrustImpl

[androidJvm]\
class [OpenId4VpReaderTrustImpl](index.md)(var readerTrustStore: ReaderTrustStore? = null) : [OpenId4VpReaderTrust](../-open-id4-vp-reader-trust/index.md)

## Constructors

| | |
|---|---|
| [OpenId4VpReaderTrustImpl](-open-id4-vp-reader-trust-impl.md) | [androidJvm]<br>constructor(readerTrustStore: ReaderTrustStore? = null) |

## Properties

| Name | Summary |
|---|---|
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>open override var [readerTrustStore](reader-trust-store.md): ReaderTrustStore? |
| [result](result.md) | [androidJvm]<br>open override val [result](result.md): [ReaderTrustResult](../-reader-trust-result/index.md) |

## Functions

| Name | Summary |
|---|---|
| [isTrusted](is-trusted.md) | [androidJvm]<br>open override fun [isTrusted](is-trusted.md)(chain: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
