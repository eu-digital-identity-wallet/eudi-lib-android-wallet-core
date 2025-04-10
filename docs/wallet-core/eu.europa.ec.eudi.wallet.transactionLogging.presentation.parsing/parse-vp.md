//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[parseVp](parse-vp.md)

# parseVp

[androidJvm]\
fun [parseVp](parse-vp.md)(rawResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html), metadata: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;?): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedDocument](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presented-document/index.md)&gt;

Function to parse the Verifiable Presentation (VP) response. It takes the raw response as a byte array and metadata as a list of strings. It returns a list of PresentedDocument objects.

#### Return

a list of PresentedDocument objects

#### Parameters

androidJvm

| | |
|---|---|
| rawResponse | the raw response to parse |
| metadata | optional metadata associated with the documents |
