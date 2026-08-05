//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[TrustPolicy](index.md)

# TrustPolicy

[androidJvm]\
fun interface [TrustPolicy](index.md)

Defines the policy for how the wallet should handle issuer trust verification results.

A [TrustPolicy](index.md) determines the [Action](-action/index.md) to take based on the type of credential (AttestationIdentifier) being issued and the optional VerificationContext.

Use [uniform](-companion/uniform.md) for a single action regardless of input, or [build](-companion/build.md) for a fine-grained policy with per-attestation and per-context overrides.

## Types

| Name | Summary |
|---|---|
| [Action](-action/index.md) | [androidJvm]<br>enum [Action](-action/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TrustPolicy.Action](-action/index.md)&gt; <br>Describes how the wallet should react to trust verification outcomes. |
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)<br>DSL builder for constructing a [TrustPolicy](index.md) with layered override rules. |
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [resolve](resolve.md) | [androidJvm]<br>abstract fun [resolve](resolve.md)(attestationIdentifier: AttestationIdentifier, verificationContext: VerificationContext?): [TrustPolicy.Action](-action/index.md)<br>Resolves the trust action for a given attestation identifier and verification context. |
