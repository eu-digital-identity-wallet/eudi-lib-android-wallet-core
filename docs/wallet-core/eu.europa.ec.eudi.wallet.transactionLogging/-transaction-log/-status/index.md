//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../../index.md)/[TransactionLog](../index.md)/[Status](index.md)

# Status

[androidJvm]\
enum [Status](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.Status](index.md)&gt; 

Represents the status of the transaction.

- 
   [Incomplete](-incomplete/index.md) indicates that the transaction is incomplete.
- 
   [Completed](-completed/index.md) indicates that the transaction is completed successfully.
- 
   [Error](-error/index.md) indicates that there was an error during the transaction.

## Entries

| | |
|---|---|
| [Incomplete](-incomplete/index.md) | [androidJvm]<br>[Incomplete](-incomplete/index.md) |
| [Completed](-completed/index.md) | [androidJvm]<br>[Completed](-completed/index.md) |
| [Error](-error/index.md) | [androidJvm]<br>[Error](-error/index.md) |

## Properties

| Name | Summary |
|---|---|
| [entries](entries.md) | [androidJvm]<br>val [entries](entries.md): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[TransactionLog.Status](index.md)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |
| [name](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-jws-algorithm/-ed448/index.md#-372974862%2FProperties%2F1615067946) | [androidJvm]<br>val [name](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-jws-algorithm/-ed448/index.md#-372974862%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [ordinal](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-jws-algorithm/-ed448/index.md#-739389684%2FProperties%2F1615067946) | [androidJvm]<br>val [ordinal](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-jws-algorithm/-ed448/index.md#-739389684%2FProperties%2F1615067946): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [androidJvm]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [TransactionLog.Status](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [androidJvm]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[TransactionLog.Status](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |
