//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[MsoMdocStatusReferenceExtractor](index.md)

# MsoMdocStatusReferenceExtractor

[release]\
object [MsoMdocStatusReferenceExtractor](index.md) : [StatusReferenceExtractor](../-status-reference-extractor/index.md)

Implements [StatusReferenceExtractor](../-status-reference-extractor/index.md) for MSO MDOC format. Extracts the status reference from the issuerAuth data of the document.

## Functions

| Name | Summary |
|---|---|
| [extractStatusReference](extract-status-reference.md) | [release]<br>open suspend override fun [extractStatusReference](extract-status-reference.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;StatusReference&gt;<br>Parses the MSO document and extracts the status reference. If the document format is not MsoMdocFormat, it throws an [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html). If the status reference cannot be extracted, it returns a failure result. |