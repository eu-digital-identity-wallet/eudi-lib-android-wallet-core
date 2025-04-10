//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[parseMsoMdoc](parse-mso-mdoc.md)

# parseMsoMdoc

[androidJvm]\
fun [parseMsoMdoc](parse-mso-mdoc.md)(rawResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html), sessionTranscript: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, metadata: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;?): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedDocument](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presented-document/index.md)&gt;

Parses the MSO mdoc response and returns a list of presented documents.

#### Return

A list of presented documents.

#### Parameters

androidJvm

| | |
|---|---|
| rawResponse | The raw response byte array from the device. |
| sessionTranscript | The session transcript byte array, or null if not available. |
| metadata | A list of metadata strings, or null if not available. |
