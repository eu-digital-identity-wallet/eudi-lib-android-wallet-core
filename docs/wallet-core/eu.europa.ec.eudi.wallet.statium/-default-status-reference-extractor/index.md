//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[DefaultStatusReferenceExtractor](index.md)

# DefaultStatusReferenceExtractor

object [DefaultStatusReferenceExtractor](index.md) : [StatusReferenceExtractor](../-status-reference-extractor/index.md)

Default implementation of [StatusReferenceExtractor](../-status-reference-extractor/index.md) It supports the following formats:

- 
   MsoMdocFormat
- 
   SdJwtVcFormat

It delegates the extraction to the appropriate extractor based on the document format.

#### See also

| |
|---|
| [MsoMdocStatusReferenceExtractor](../-mso-mdoc-status-reference-extractor/index.md) |
| [SdJwtStatusReferenceExtractor](../-sd-jwt-status-reference-extractor/index.md) |

## Functions

| Name | Summary |
|---|---|
| [extractStatusReference](extract-status-reference.md) | [androidJvm]<br>open suspend override fun [extractStatusReference](extract-status-reference.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;StatusReference&gt;<br>Extracts status reference from the provided document |
