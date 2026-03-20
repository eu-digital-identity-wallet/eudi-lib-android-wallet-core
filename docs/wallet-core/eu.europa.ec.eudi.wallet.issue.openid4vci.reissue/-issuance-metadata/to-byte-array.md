//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.reissue](../index.md)/[IssuanceMetadata](index.md)/[toByteArray](to-byte-array.md)

# toByteArray

[androidJvm]\
fun [toByteArray](to-byte-array.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)

Serializes this [IssuanceMetadata](index.md) to a [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html) for storage.

This method converts the issuance metadata into a JSON-encoded ByteArray that can be stored in a multipaz Storage table. Use [fromByteArray](-companion/from-byte-array.md) to deserialize.

#### Return

The serialized ByteArray
