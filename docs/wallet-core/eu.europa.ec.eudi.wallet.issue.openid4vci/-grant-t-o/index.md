//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[GrantTO](index.md)

# GrantTO

[androidJvm]\
@Serializable

enum [GrantTO](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[GrantTO](index.md)&gt;

## Entries

| | |
|---|---|
| [AuthorizationCode](-authorization-code/index.md) | [androidJvm]<br>@SerialName(value = &quot;authorization_code&quot;)<br>[AuthorizationCode](-authorization-code/index.md) |
| [PreAuthorizedCodeGrant](-pre-authorized-code-grant/index.md) | [androidJvm]<br>@SerialName(value = &quot;urn:ietf:params:oauth:grant-type:pre-authorized_code&quot;)<br>[PreAuthorizedCodeGrant](-pre-authorized-code-grant/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [entries](entries.md) | [androidJvm]<br>val [entries](entries.md): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[GrantTO](index.md)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |
| [name](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-372974862%2FProperties%2F1615067946) | [androidJvm]<br>val [name](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-372974862%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [ordinal](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-739389684%2FProperties%2F1615067946) | [androidJvm]<br>val [ordinal](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-encryption-method/-x-c20-p/index.md#-739389684%2FProperties%2F1615067946): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [toGrant](to-grant.md) | [androidJvm]<br>fun [toGrant](to-grant.md)(): Grant |
| [valueOf](value-of.md) | [androidJvm]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [GrantTO](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [androidJvm]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[GrantTO](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |
