//[transfer-manager](../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [ReaderTrustStore](-reader-trust-store/index.md) | [release]<br>interface [ReaderTrustStore](-reader-trust-store/index.md)<br>Interface that defines a trust manager, used to check the validity of a document signer and the associated certificate chain. |
| [ReaderTrustStoreAware](-reader-trust-store-aware/index.md) | [release]<br>interface [ReaderTrustStoreAware](-reader-trust-store-aware/index.md)<br>Interface that indicates that implementation are aware of a [ReaderTrustStore](-reader-trust-store/index.md) that can be used to verify the authenticity of a reader. |
| [ReaderTrustStoreImpl](-reader-trust-store-impl/index.md) | [release]<br>class [ReaderTrustStoreImpl](-reader-trust-store-impl/index.md)(trustedCertificates: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, profileValidation: [ProfileValidation](../eu.europa.ec.eudi.iso18013.transfer.readerauth.profile/-profile-validation/index.md), errorLogger: (tag: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html) = { tag, message, cause -&gt;         Log.d(tag, message, cause)     }, revocationPolicy: [RevocationPolicy](-revocation-policy/index.md) = RevocationPolicy.NoCheck) : [ReaderTrustStore](-reader-trust-store/index.md) |
| [RevocationPolicy](-revocation-policy/index.md) | [release]<br>sealed interface [RevocationPolicy](-revocation-policy/index.md)<br>Policy that controls how certificate revocation is checked during reader authentication trust path validation. |