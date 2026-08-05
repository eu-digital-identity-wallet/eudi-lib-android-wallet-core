//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document.metadata](../../index.md)/[IssuerMetadata](../index.md)/[Companion](index.md)/[fromJson](from-json.md)

# fromJson

[release]\
fun [fromJson](from-json.md)(json: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[IssuerMetadata](../index.md)&gt;

Create a [IssuerMetadata](../index.md) object from a JSON string.

#### Return

the [IssuerMetadata](../index.md) object

#### Parameters

release

| | |
|---|---|
| json | the JSON string representation of the object |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if the decoded input cannot be represented as a valid instance of [IssuerMetadata](../index.md) |
| SerializationException | if the given JSON string is not a valid JSON input |