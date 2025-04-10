//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[parseRawResponse](parse-raw-response.md)

# parseRawResponse

[androidJvm]\
fun [parseRawResponse](parse-raw-response.md)(rawResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-pair/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;VerifiablePresentation.Generic&gt;, PresentationSubmission&gt;?

Function to parse the raw response of Vp. It takes the raw response as a byte array and returns a pair of verifiable presentations and presentation submission.

#### Return

a pair of verifiable presentations and presentation submission or null if parsing fails

#### Parameters

androidJvm

| | |
|---|---|
| rawResponse | the raw response to parse |
