//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.util](../index.md)/[CBOR](index.md)

# CBOR

[androidJvm]\
object [CBOR](index.md)

## Functions

| Name | Summary |
|---|---|
| [cborDecode](cbor-decode.md) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [cborDecode](cbor-decode.md)(encodedBytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): DataItem<br>Decodes a given CBOR byte array into a DataItem. |
| [cborDecodeByteString](cbor-decode-byte-string.md) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [cborDecodeByteString](cbor-decode-byte-string.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)<br>Decodes a given CBOR byte array into a byte array. |
| [cborEncode](cbor-encode.md) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [cborEncode](cbor-encode.md)(dataItem: DataItem): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)<br>Encodes a given DataItem into a CBOR byte array. |
| [cborParse](cbor-parse.md) | [androidJvm]<br>fun [cborParse](cbor-parse.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?<br>Parses a given CBOR byte array into a Kotlin object. |
| [cborPrettyPrint](cbor-pretty-print.md) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [cborPrettyPrint](cbor-pretty-print.md)(encodedBytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>Pretty prints a given CBOR byte array. |
