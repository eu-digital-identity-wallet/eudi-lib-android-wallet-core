//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[ReaderTrustResult](index.md)

# ReaderTrustResult

sealed interface [ReaderTrustResult](index.md)

#### Inheritors

| |
|---|
| [Processed](-processed/index.md) |
| [Pending](-pending/index.md) |

## Types

| Name | Summary |
|---|---|
| [Pending](-pending/index.md) | [release]<br>data object [Pending](-pending/index.md) : [ReaderTrustResult](index.md) |
| [Processed](-processed/index.md) | [release]<br>data class [Processed](-processed/index.md)(val chain: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, val isTrusted: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)) : [ReaderTrustResult](index.md) |