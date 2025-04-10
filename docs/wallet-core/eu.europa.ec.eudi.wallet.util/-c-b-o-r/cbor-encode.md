//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.util](../index.md)/[CBOR](index.md)/[cborEncode](cbor-encode.md)

# cborEncode

[androidJvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [cborEncode](cbor-encode.md)(dataItem: DataItem): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)

Encodes a given DataItem into a CBOR byte array.

#### Return

A byte array representing the encoded CBOR data.

#### Parameters

androidJvm

| | |
|---|---|
| dataItem | The DataItem to encode. |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | If encoding fails. |
