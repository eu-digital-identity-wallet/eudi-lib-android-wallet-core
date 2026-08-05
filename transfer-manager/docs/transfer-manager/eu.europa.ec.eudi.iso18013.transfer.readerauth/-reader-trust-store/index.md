//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../index.md)/[ReaderTrustStore](index.md)

# ReaderTrustStore

interface [ReaderTrustStore](index.md)

Interface that defines a trust manager, used to check the validity of a document signer and the associated certificate chain.

Note that each document type should have a different trust manager; this trust manager is selected by OID in the DS certificate. These trust managers should have a specific TrustStore for each certificate and may implement specific checks required for the document type.

#### Inheritors

| |
|---|
| [ReaderTrustStoreImpl](../-reader-trust-store-impl/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [createCertificationTrustPath](create-certification-trust-path.md) | [release]<br>abstract fun [createCertificationTrustPath](create-certification-trust-path.md)(chain: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;?<br>This method creates a certification trust path by finding a certificate in the trust store that is the issuer of a certificate in the certificate chain. It returns `null` if no trusted certificate can be found. |
| [validateCertificationTrustPath](validate-certification-trust-path.md) | [release]<br>abstract fun [validateCertificationTrustPath](validate-certification-trust-path.md)(chainToDocumentSigner: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>This method validates that the given certificate chain is a valid chain that includes a document signer. Accepts a chain of certificates, starting with the document signer certificate, followed by any intermediate certificates up to the optional root certificate. |