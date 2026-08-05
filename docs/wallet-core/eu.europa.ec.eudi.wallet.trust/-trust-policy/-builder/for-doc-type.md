//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../../index.md)/[TrustPolicy](../index.md)/[Builder](index.md)/[forDocType](for-doc-type.md)

# forDocType

[androidJvm]\
fun [forDocType](for-doc-type.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), action: [TrustPolicy.Action](../-action/index.md)): &lt;Error class: unknown class&gt;

Convenience method to add an override for an MDoc document type.

Equivalent to `forAttestation(AttestationIdentifier.MDoc(docType), action)`.

#### Return

this builder for chaining

#### Parameters

androidJvm

| | |
|---|---|
| docType | the MDoc document type string |
| action | the action to return when the document type matches |
