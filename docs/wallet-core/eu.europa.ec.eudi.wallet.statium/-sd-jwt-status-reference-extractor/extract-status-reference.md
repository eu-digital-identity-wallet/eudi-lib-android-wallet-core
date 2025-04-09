//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[SdJwtStatusReferenceExtractor](index.md)/[extractStatusReference](extract-status-reference.md)

# extractStatusReference

[androidJvm]\
open suspend override fun [extractStatusReference](extract-status-reference.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;StatusReference&gt;

Extracts the status reference from the given [document](extract-status-reference.md). If the document is not in the SdJwtVcFormat, returns a Failure. If status list is not found, returns a Failure.

#### Return

the status reference

#### Parameters

androidJvm

| | |
|---|---|
| document | the issued document |
