//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.zkp](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [Circuit](-circuit/index.md) | [release]<br>data class [Circuit](-circuit/index.md)(val filename: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val bytes: ByteString)<br>Data class representing a circuit with its filename and byte content. |
| [LongfellowCircuits](-longfellow-circuits/index.md) | [release]<br>object [LongfellowCircuits](-longfellow-circuits/index.md)<br>Default Longfellow circuits. |
| [LongfellowZkSystemRepository](-longfellow-zk-system-repository/index.md) | [release]<br>class [LongfellowZkSystemRepository](-longfellow-zk-system-repository/index.md)(circuits: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Circuit](-circuit/index.md)&gt;)<br>Repository builder for Longfellow zero-knowledge proof systems. |