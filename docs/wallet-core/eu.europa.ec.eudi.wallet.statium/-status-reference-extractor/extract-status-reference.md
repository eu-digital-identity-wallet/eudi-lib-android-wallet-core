//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[StatusReferenceExtractor](index.md)/[extractStatusReference](extract-status-reference.md)

# extractStatusReference

[release]\
abstract suspend fun [extractStatusReference](extract-status-reference.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;StatusReference&gt;

Extracts revocation status data from the provided document

#### Return

Result containing the extracted revocation status data or an error

#### Parameters

release

| | |
|---|---|
| document | The document to extract revocation status data from |