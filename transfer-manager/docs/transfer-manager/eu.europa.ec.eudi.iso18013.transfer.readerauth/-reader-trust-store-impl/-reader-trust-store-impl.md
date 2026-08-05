//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../index.md)/[ReaderTrustStoreImpl](index.md)/[ReaderTrustStoreImpl](-reader-trust-store-impl.md)

# ReaderTrustStoreImpl

[release]\
constructor(trustedCertificates: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, profileValidation: [ProfileValidation](../../eu.europa.ec.eudi.iso18013.transfer.readerauth.profile/-profile-validation/index.md), errorLogger: (tag: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html) = { tag, message, cause -&gt;
        Log.d(tag, message, cause)
    }, revocationPolicy: [RevocationPolicy](../-revocation-policy/index.md) = RevocationPolicy.NoCheck)