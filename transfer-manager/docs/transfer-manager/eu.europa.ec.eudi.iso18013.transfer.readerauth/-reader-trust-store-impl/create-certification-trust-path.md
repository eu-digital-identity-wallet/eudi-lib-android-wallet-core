//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../index.md)/[ReaderTrustStoreImpl](index.md)/[createCertificationTrustPath](create-certification-trust-path.md)

# createCertificationTrustPath

[release]\
open override fun [createCertificationTrustPath](create-certification-trust-path.md)(chain: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;?

This method creates a certification trust path by finding a certificate in the trust store that is the issuer of a certificate in the certificate chain. It returns `null` if no trusted certificate can be found.

#### Return

the certification path in the same order, or null if no certification trust path could be created

#### Parameters

release

| | |
|---|---|
| chain | the chain, leaf certificate first, followed by any certificate that signed the previous certificate |