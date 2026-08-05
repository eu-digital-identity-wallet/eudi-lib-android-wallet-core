//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../../index.md)/[ReaderTrustStore](../index.md)/[Companion](index.md)

# Companion

[release]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [getDefault](get-default.md) | [release]<br>@[JvmStatic](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [getDefault](get-default.md)(trustedCertificates: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [ReaderTrustStore](../index.md)<br>Returns a default trust store that uses the given list of trusted certificates. Revocation checking is disabled (NoCheck) for backwards compatibility.<br>[release]<br>@[JvmStatic](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [getDefault](get-default.md)(trustedCertificates: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, revocationPolicy: [RevocationPolicy](../../-revocation-policy/index.md)): [ReaderTrustStore](../index.md)<br>Returns a default trust store that uses the given list of trusted certificates and the specified revocation policy. |