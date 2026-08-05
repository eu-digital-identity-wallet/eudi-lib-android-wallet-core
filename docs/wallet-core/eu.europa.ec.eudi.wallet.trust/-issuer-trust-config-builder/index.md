//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[IssuerTrustConfigBuilder](index.md)

# IssuerTrustConfigBuilder

[androidJvm]\
class [IssuerTrustConfigBuilder](index.md)

DSL builder for constructing an IssuerTrustConfig.

A trust source must be provided via one of the [trustSource](trust-source.md) overloads. When using IsChainTrustedForEUDIW or ComposeChainTrust as the trust source, [classifications](classifications.md) must also be provided so the builder can construct an IsChainTrustedForAttestation instance.

Example:

```kotlin
val config = IssuerTrustConfigBuilder().apply {
    trustSource(myComposeChainTrust)
    classifications(myClassifications)
    policy {
        default(TrustPolicy.Action.ENFORCE)
        forContext(VerificationContext.PID, TrustPolicy.Action.INFORM)
    }
}.build()
```

## Constructors

| | |
|---|---|
| [IssuerTrustConfigBuilder](-issuer-trust-config-builder.md) | [androidJvm]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [classifications](classifications.md) | [androidJvm]<br>fun [classifications](classifications.md)(classifications: AttestationClassifications)<br>Sets the attestation classifications used to map credential types to verification contexts. |
| [credentialTrustVerifier](credential-trust-verifier.md) | [androidJvm]<br>fun [credentialTrustVerifier](credential-trust-verifier.md)(format: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;out DocumentFormat&gt;, verifier: [CredentialTrustVerifier](../-credential-trust-verifier/index.md))<br>Registers a custom [CredentialTrustVerifier](../-credential-trust-verifier/index.md) for a specific DocumentFormat type. |
| [policy](policy.md) | [androidJvm]<br>fun [policy](policy.md)(block: [TrustPolicy.Builder](../-trust-policy/-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html))<br>Configures the trust policy using the [TrustPolicy.Builder](../-trust-policy/-builder/index.md) DSL. |
| [trustSource](trust-source.md) | [androidJvm]<br>fun [trustSource](trust-source.md)(source: &lt;Error class: unknown class&gt;&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, [TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;)<br>Sets the trust source from an IsChainTrustedForEUDIW instance.<br>[androidJvm]<br>fun [trustSource](trust-source.md)(source: ComposeChainTrust&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, VerificationContext, [TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;)<br>Sets the trust source from a ComposeChainTrust instance.<br>[androidJvm]<br>fun [trustSource](trust-source.md)(source: IsChainTrustedForAttestation&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, [TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;)<br>Sets the trust source from a pre-built IsChainTrustedForAttestation. |
