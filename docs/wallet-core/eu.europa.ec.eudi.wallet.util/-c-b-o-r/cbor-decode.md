//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.util](../index.md)/[CBOR](index.md)/[cborDecode](cbor-decode.md)

# cborDecode

[androidJvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [cborDecode](cbor-decode.md)(encodedBytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): DataItem

Decodes a given CBOR byte array into a DataItem.

#### Return

The decoded DataItem.

#### Parameters

androidJvm

| | |
|---|---|
| encodedBytes | The byte array to decode. |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | If decoding fails or the number of decoded items is not 1. |
