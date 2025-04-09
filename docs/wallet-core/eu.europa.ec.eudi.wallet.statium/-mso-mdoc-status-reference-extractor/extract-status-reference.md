//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[MsoMdocStatusReferenceExtractor](index.md)/[extractStatusReference](extract-status-reference.md)

# extractStatusReference

[androidJvm]\
open suspend override fun [extractStatusReference](extract-status-reference.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;StatusReference&gt;

Parses the MSO document and extracts the status reference. If the document format is not MsoMdocFormat, it throws an [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html). If the status reference cannot be extracted, it returns a failure result.

#### Return

A [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) containing the extracted StatusReference or an exception if the extraction fails.

#### Parameters

androidJvm

| | |
|---|---|
| document | The issued document to extract the status reference from. |
