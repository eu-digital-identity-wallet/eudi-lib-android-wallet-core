//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[StatusReferenceExtractor](index.md)

# StatusReferenceExtractor

interface [StatusReferenceExtractor](index.md)

Interface for extracting revocation status data from documents

#### Inheritors

| |
|---|
| [MsoMdocStatusReferenceExtractor](../-mso-mdoc-status-reference-extractor/index.md) |
| [SdJwtStatusReferenceExtractor](../-sd-jwt-status-reference-extractor/index.md) |
| [DefaultStatusReferenceExtractor](../-default-status-reference-extractor/index.md) |

## Functions

| Name | Summary |
|---|---|
| [extractStatusReference](extract-status-reference.md) | [androidJvm]<br>abstract suspend fun [extractStatusReference](extract-status-reference.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;StatusReference&gt;<br>Extracts revocation status data from the provided document |
