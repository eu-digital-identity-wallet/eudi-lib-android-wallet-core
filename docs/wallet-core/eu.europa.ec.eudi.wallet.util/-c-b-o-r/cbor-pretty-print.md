//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.util](../index.md)/[CBOR](index.md)/[cborPrettyPrint](cbor-pretty-print.md)

# cborPrettyPrint

[androidJvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [cborPrettyPrint](cbor-pretty-print.md)(encodedBytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)

Pretty prints a given CBOR byte array.

#### Return

A string representing the pretty-printed CBOR data.

#### Parameters

androidJvm

| | |
|---|---|
| encodedBytes | The CBOR byte array to pretty print. |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | If decoding fails. |
