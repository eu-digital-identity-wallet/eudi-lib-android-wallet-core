//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../../index.md)/[TrustPolicy](../index.md)/[Action](index.md)

# Action

[androidJvm]\
enum [Action](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TrustPolicy.Action](index.md)&gt; 

Describes how the wallet should react to trust verification outcomes.

## Entries

| | |
|---|---|
| [ENFORCE](-e-n-f-o-r-c-e/index.md) | [androidJvm]<br>[ENFORCE](-e-n-f-o-r-c-e/index.md)<br>Strict enforcement: if the issuer is not trusted, reject and delete the document and emit a `DocumentFailed` event. |
| [INFORM](-i-n-f-o-r-m/index.md) | [androidJvm]<br>[INFORM](-i-n-f-o-r-m/index.md)<br>Informational only: always store the document regardless of trust result, and attach the trust verification result to the `DocumentIssued` event. |

## Properties

| Name | Summary |
|---|---|
| [entries](entries.md) | [androidJvm]<br>val [entries](entries.md): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[TrustPolicy.Action](index.md)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |
| [name](-i-n-f-o-r-m/index.md#-372974862%2FProperties%2F1615067946) | [androidJvm]<br>val [name](-i-n-f-o-r-m/index.md#-372974862%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [ordinal](-i-n-f-o-r-m/index.md#-739389684%2FProperties%2F1615067946) | [androidJvm]<br>val [ordinal](-i-n-f-o-r-m/index.md#-739389684%2FProperties%2F1615067946): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [androidJvm]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [TrustPolicy.Action](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [androidJvm]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[TrustPolicy.Action](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |
