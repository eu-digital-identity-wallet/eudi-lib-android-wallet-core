//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[IssuerTrustConfigBuilder](index.md)/[trustSource](trust-source.md)

# trustSource

[androidJvm]\
fun [trustSource](trust-source.md)(source: IsChainTrustedForAttestation&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, [TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;)

Sets the trust source from a pre-built IsChainTrustedForAttestation.

When using this overload, [classifications](classifications.md) is not required (the attestation already encapsulates the classification logic).

#### Parameters

androidJvm

| | |
|---|---|
| source | the pre-built attestation trust source |

[androidJvm]\
fun [trustSource](trust-source.md)(source: ComposeChainTrust&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, VerificationContext, [TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;)

Sets the trust source from a ComposeChainTrust instance.

When using this overload, [classifications](classifications.md) must be provided before calling build.

#### Parameters

androidJvm

| | |
|---|---|
| source | the composed chain trust source |

[androidJvm]\
fun [trustSource](trust-source.md)(source: &lt;Error class: unknown class&gt;&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, [TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;)

Sets the trust source from an IsChainTrustedForEUDIW instance.

When using this overload, [classifications](classifications.md) must be provided before calling build.

#### Parameters

androidJvm

| | |
|---|---|
| source | the EUDIW trust source |
