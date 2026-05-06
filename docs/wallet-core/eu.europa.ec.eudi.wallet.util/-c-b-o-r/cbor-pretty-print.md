//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.util](../index.md)/[CBOR](index.md)/[cborPrettyPrint](cbor-pretty-print.md)

# cborPrettyPrint

[release]\

@[JvmStatic](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [cborPrettyPrint](cbor-pretty-print.md)(encodedBytes: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)

Pretty prints a given CBOR byte array.

#### Return

A string representing the pretty-printed CBOR data.

#### Parameters

release

| | |
|---|---|
| encodedBytes | The CBOR byte array to pretty print. |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-state-exception/index.html) | If decoding fails. |