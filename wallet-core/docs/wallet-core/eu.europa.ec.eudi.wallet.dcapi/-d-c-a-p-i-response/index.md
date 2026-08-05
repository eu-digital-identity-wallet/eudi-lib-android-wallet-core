//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIResponse](index.md)

# DCAPIResponse

[release]\
data class [DCAPIResponse](index.md)(val deviceResponseBytes: DeviceResponseBytes, val intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html), val documentIds: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;) : Response

Represents a response for the Digital Credential API (DCAPI).

## Constructors

| | |
|---|---|
| [DCAPIResponse](-d-c-a-p-i-response.md) | [release]<br>constructor(deviceResponseBytes: DeviceResponseBytes, intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html), documentIds: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;) |

## Properties

| Name | Summary |
|---|---|
| [deviceResponseBytes](device-response-bytes.md) | [release]<br>val [deviceResponseBytes](device-response-bytes.md): DeviceResponseBytes<br>The bytes of the device response. |
| [documentIds](document-ids.md) | [release]<br>val [documentIds](document-ids.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;<br>The list of document ids in response indexed as positioned in CBOR array in responseBytes. |
| [intent](intent.md) | [release]<br>val [intent](intent.md): [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html)<br>The intent associated with the response. |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [release]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [release]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |