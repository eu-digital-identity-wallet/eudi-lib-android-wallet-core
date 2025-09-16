//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIResponse](index.md)

# DCAPIResponse

[androidJvm]\
data class [DCAPIResponse](index.md)(val deviceResponseBytes: &lt;Error class: unknown class&gt;, val intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html), val documentIds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;&lt;Error class: unknown class&gt;&gt;) : Response

Represents a response for the Digital Credential API (DCAPI).

## Constructors

| | |
|---|---|
| [DCAPIResponse](-d-c-a-p-i-response.md) | [androidJvm]<br>constructor(deviceResponseBytes: &lt;Error class: unknown class&gt;, intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html), documentIds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;&lt;Error class: unknown class&gt;&gt;) |

## Properties

| Name | Summary |
|---|---|
| [deviceResponseBytes](device-response-bytes.md) | [androidJvm]<br>val [deviceResponseBytes](device-response-bytes.md): &lt;Error class: unknown class&gt;<br>The bytes of the device response. |
| [documentIds](document-ids.md) | [androidJvm]<br>val [documentIds](document-ids.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;&lt;Error class: unknown class&gt;&gt;<br>The list of document ids in response indexed as positioned in CBOR array in responseBytes. |
| [intent](intent.md) | [androidJvm]<br>val [intent](intent.md): [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html)<br>The intent associated with the response. |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [androidJvm]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [androidJvm]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
