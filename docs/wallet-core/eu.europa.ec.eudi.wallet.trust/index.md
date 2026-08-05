//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.trust](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [CredentialTrustVerifier](-credential-trust-verifier/index.md) | [androidJvm]<br>fun interface [CredentialTrustVerifier](-credential-trust-verifier/index.md)<br>Per-format credential trust verifier. Implementations extract the certificate chain from the credential and evaluate trust using the ETSI library. |
| [IssuerNotTrustedException](-issuer-not-trusted-exception/index.md) | [androidJvm]<br>class [IssuerNotTrustedException](-issuer-not-trusted-exception/index.md)(cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)) : [Exception](https://developer.android.com/reference/kotlin/java/lang/Exception.html)<br>Thrown when the issuer certificate chain is not trusted and the [trust policy](-trust-policy/index.md) action is [TrustPolicy.Action.ENFORCE](-trust-policy/-action/-e-n-f-o-r-c-e/index.md). |
| [IssuerTrustConfigBuilder](-issuer-trust-config-builder/index.md) | [androidJvm]<br>class [IssuerTrustConfigBuilder](-issuer-trust-config-builder/index.md)<br>DSL builder for constructing an IssuerTrustConfig. |
| [TrustPolicy](-trust-policy/index.md) | [androidJvm]<br>fun interface [TrustPolicy](-trust-policy/index.md)<br>Defines the policy for how the wallet should handle issuer trust verification results. |
