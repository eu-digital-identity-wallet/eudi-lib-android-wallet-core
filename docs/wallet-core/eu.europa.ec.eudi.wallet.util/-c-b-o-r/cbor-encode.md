//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.util](../index.md)/[CBOR](index.md)/[cborEncode](cbor-encode.md)

# cborEncode

[release]\

@[JvmStatic](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [cborEncode](cbor-encode.md)(dataItem: DataItem): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)

Encodes a given DataItem into a CBOR byte array.

#### Return

A byte array representing the encoded CBOR data.

#### Parameters

release

| | |
|---|---|
| dataItem | The DataItem to encode. |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-state-exception/index.html) | If encoding fails. |