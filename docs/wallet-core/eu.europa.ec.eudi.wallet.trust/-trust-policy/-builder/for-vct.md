//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../../index.md)/[TrustPolicy](../index.md)/[Builder](index.md)/[forVct](for-vct.md)

# forVct

[androidJvm]\
fun [forVct](for-vct.md)(vct: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;

Convenience method to add an override for an SD-JWT VC type.

Equivalent to `forAttestation(AttestationIdentifier.SDJwtVc(vct), action)`.

#### Return

this builder for chaining

#### Parameters

androidJvm

| | |
|---|---|
| vct | the SD-JWT Verifiable Credential Type |
| action | the action to return when the VCT matches |
