//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[IssuerTrustConfigBuilder](index.md)/[credentialTrustVerifier](credential-trust-verifier.md)

# credentialTrustVerifier

[androidJvm]\
fun [credentialTrustVerifier](credential-trust-verifier.md)(format: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;out DocumentFormat&gt;, verifier: [CredentialTrustVerifier](../-credential-trust-verifier/index.md))

Registers a custom [CredentialTrustVerifier](../-credential-trust-verifier/index.md) for a specific DocumentFormat type.

#### Parameters

androidJvm

| | |
|---|---|
| format | the document format class to associate the verifier with |
| verifier | the credential trust verifier implementation |
