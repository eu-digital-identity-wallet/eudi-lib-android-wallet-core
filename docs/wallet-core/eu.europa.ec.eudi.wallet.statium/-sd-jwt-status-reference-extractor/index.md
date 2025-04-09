//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[SdJwtStatusReferenceExtractor](index.md)

# SdJwtStatusReferenceExtractor

[androidJvm]\
object [SdJwtStatusReferenceExtractor](index.md) : [StatusReferenceExtractor](../-status-reference-extractor/index.md)

Extracts the status reference from an SD-JWT VC.

## Functions

| Name | Summary |
|---|---|
| [extractStatusReference](extract-status-reference.md) | [androidJvm]<br>open suspend override fun [extractStatusReference](extract-status-reference.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;StatusReference&gt;<br>Extracts the status reference from the given [document](extract-status-reference.md). If the document is not in the SdJwtVcFormat, returns a Failure. If status list is not found, returns a Failure. |
