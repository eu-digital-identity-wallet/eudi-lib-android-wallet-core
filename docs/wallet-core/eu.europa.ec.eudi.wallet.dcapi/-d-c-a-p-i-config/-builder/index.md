//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../../index.md)/[DCAPIConfig](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)

Builder for [DCAPIConfig](../index.md).

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [enabled](enabled.md) | [androidJvm]<br>var [enabled](enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>whether the DCAPI is enabled, defaults to false |
| [privilegedAllowlist](privileged-allowlist.md) | [androidJvm]<br>var [privilegedAllowlist](privileged-allowlist.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>the privileged allowlist for the DCAPI |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [DCAPIConfig](../index.md) |
| [withEnabled](with-enabled.md) | [androidJvm]<br>fun [withEnabled](with-enabled.md)(enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): &lt;Error class: unknown class&gt;<br>Sets whether the DCAPI is enabled. |
| [withPrivilegedAllowlist](with-privileged-allowlist.md) | [androidJvm]<br>fun [withPrivilegedAllowlist](with-privileged-allowlist.md)(allowlist: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): &lt;Error class: unknown class&gt;<br>Sets the privileged allowlist for the DCAPI. |
