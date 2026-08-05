//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../../index.md)/[TrustPolicy](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)

DSL builder for constructing a [TrustPolicy](../index.md) with layered override rules.

Resolution order (highest priority first):

1. 
   Per-attestation overrides (added via [forAttestation](for-attestation.md), [forDocType](for-doc-type.md), or [forVct](for-vct.md))
2. 
   Per-context overrides (added via [forContext](for-context.md))
3. 
   Default action (set via [default](default.md), defaults to [Action.ENFORCE](../-action/-e-n-f-o-r-c-e/index.md))

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [TrustPolicy](../index.md)<br>Builds the [TrustPolicy](../index.md) with the configured overrides. |
| [default](default.md) | [androidJvm]<br>fun [default](default.md)(action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;<br>Sets the default action when no specific override matches. |
| [forAttestation](for-attestation.md) | [androidJvm]<br>fun [forAttestation](for-attestation.md)(identifier: AttestationIdentifier, action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;<br>Adds an override for a specific AttestationIdentifier. |
| [forContext](for-context.md) | [androidJvm]<br>fun [forContext](for-context.md)(context: VerificationContext, action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;<br>Adds an override for a specific VerificationContext. |
| [forDocType](for-doc-type.md) | [androidJvm]<br>fun [forDocType](for-doc-type.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;<br>Convenience method to add an override for an MDoc document type. |
| [forVct](for-vct.md) | [androidJvm]<br>fun [forVct](for-vct.md)(vct: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;<br>Convenience method to add an override for an SD-JWT VC type. |
