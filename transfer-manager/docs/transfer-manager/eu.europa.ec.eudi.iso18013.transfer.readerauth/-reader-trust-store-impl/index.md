//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../index.md)/[ReaderTrustStoreImpl](index.md)

# ReaderTrustStoreImpl

[release]\
class [ReaderTrustStoreImpl](index.md)(trustedCertificates: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, profileValidation: [ProfileValidation](../../eu.europa.ec.eudi.iso18013.transfer.readerauth.profile/-profile-validation/index.md), errorLogger: (tag: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html) = { tag, message, cause -&gt;
        Log.d(tag, message, cause)
    }, revocationPolicy: [RevocationPolicy](../-revocation-policy/index.md) = RevocationPolicy.NoCheck) : [ReaderTrustStore](../-reader-trust-store/index.md)

## Constructors

| | |
|---|---|
| [ReaderTrustStoreImpl](-reader-trust-store-impl.md) | [release]<br>constructor(trustedCertificates: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, profileValidation: [ProfileValidation](../../eu.europa.ec.eudi.iso18013.transfer.readerauth.profile/-profile-validation/index.md), errorLogger: (tag: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html) = { tag, message, cause -&gt;         Log.d(tag, message, cause)     }, revocationPolicy: [RevocationPolicy](../-revocation-policy/index.md) = RevocationPolicy.NoCheck) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [createCertificationTrustPath](create-certification-trust-path.md) | [release]<br>open override fun [createCertificationTrustPath](create-certification-trust-path.md)(chain: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;?<br>This method creates a certification trust path by finding a certificate in the trust store that is the issuer of a certificate in the certificate chain. It returns `null` if no trusted certificate can be found. |
| [validateCertificationTrustPath](validate-certification-trust-path.md) | [release]<br>open override fun [validateCertificationTrustPath](validate-certification-trust-path.md)(chainToDocumentSigner: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>Validates the certification trust path of a document signer. |