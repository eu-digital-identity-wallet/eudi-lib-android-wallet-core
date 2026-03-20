//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.reissue](../../index.md)/[IssuanceMetadata](../index.md)/[Companion](index.md)/[fromByteArray](from-byte-array.md)

# fromByteArray

[androidJvm]\
fun [fromByteArray](from-byte-array.md)(bytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)): [IssuanceMetadata](../index.md)

Deserializes an [IssuanceMetadata](../index.md) from a [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html).

This method reconstructs an [IssuanceMetadata](../index.md) from its serialized JSON form. The ByteArray should have been created using [IssuanceMetadata.toByteArray](../to-byte-array.md).

#### Return

The deserialized [IssuanceMetadata](../index.md)

#### Parameters

androidJvm

| | |
|---|---|
| bytes | The serialized ByteArray |

#### Throws

| | |
|---|---|
| [Exception](https://developer.android.com/reference/kotlin/java/lang/Exception.html) | if the bytes are invalid or corrupted |
