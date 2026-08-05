//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../index.md)/[ReaderTrustStore](index.md)/[validateCertificationTrustPath](validate-certification-trust-path.md)

# validateCertificationTrustPath

[release]\
abstract fun [validateCertificationTrustPath](validate-certification-trust-path.md)(chainToDocumentSigner: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)

This method validates that the given certificate chain is a valid chain that includes a document signer. Accepts a chain of certificates, starting with the document signer certificate, followed by any intermediate certificates up to the optional root certificate.

The trust manager should be initialized with a set of trusted certificates. The chain is trusted if a trusted certificate can be found that has signed any certificate in the chain. The trusted certificate itself will be validated as well.

#### Return

false if no trusted certificate could be found for the certificate chain or if the certificate chain is invalid for any reason

#### Parameters

release

| | |
|---|---|
| chainToDocumentSigner | the document signer, intermediate certificates and optional root certificate |