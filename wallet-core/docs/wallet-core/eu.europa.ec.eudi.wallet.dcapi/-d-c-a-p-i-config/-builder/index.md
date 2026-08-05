//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../../index.md)/[DCAPIConfig](../index.md)/[Builder](index.md)

# Builder

[release]\
class [Builder](index.md)

Builder for [DCAPIConfig](../index.md).

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [enabled](enabled.md) | [release]<br>var [enabled](enabled.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>whether the DCAPI is enabled, defaults to false |
| [privilegedAllowlist](privileged-allowlist.md) | [release]<br>var [privilegedAllowlist](privileged-allowlist.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?<br>the privileged allowlist for the DCAPI |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [DCAPIConfig](../index.md) |
| [withEnabled](with-enabled.md) | [release]<br>fun [withEnabled](with-enabled.md)(enabled: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)): [DCAPIConfig.Builder](index.md)<br>Sets whether the DCAPI is enabled. |
| [withPrivilegedAllowlist](with-privileged-allowlist.md) | [release]<br>fun [withPrivilegedAllowlist](with-privileged-allowlist.md)(allowlist: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): [DCAPIConfig.Builder](index.md)<br>Sets the privileged allowlist for the DCAPI. |