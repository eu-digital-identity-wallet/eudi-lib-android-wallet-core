//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../../index.md)/[TransactionLog](../index.md)/[Type](index.md)

# Type

[release]\
@Serializable

enum [Type](index.md) : [Enum](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.Type](index.md)&gt; 

Represents the type of the transaction.

- 
   [Presentation](-presentation/index.md) indicates that the transaction is related to a presentation of documents.
- 
   [Issuance](-issuance/index.md) indicates that the transaction is related to the issuance of documents.
- 
   [Signing](-signing/index.md) indicates that the transaction is related to the signing of documents.

## Entries

| | |
|---|---|
| [Presentation](-presentation/index.md) | [release]<br>[Presentation](-presentation/index.md) |
| [Issuance](-issuance/index.md) | [release]<br>[Issuance](-issuance/index.md) |
| [Signing](-signing/index.md) | [release]<br>[Signing](-signing/index.md) |

## Properties

| Name | Summary |
|---|---|
| [entries](entries.md) | [release]<br>val [entries](entries.md): [EnumEntries](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[TransactionLog.Type](index.md)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |
| [name](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-372974862%2FProperties%2F-946843593) | [release]<br>val [name](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-372974862%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [ordinal](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-739389684%2FProperties%2F-946843593) | [release]<br>val [ordinal](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-739389684%2FProperties%2F-946843593): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [release]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): [TransactionLog.Type](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [release]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-array/index.html)&lt;[TransactionLog.Type](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |