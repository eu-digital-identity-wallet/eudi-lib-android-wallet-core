//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIConfig](index.md)

# DCAPIConfig

[androidJvm]\
class [DCAPIConfig](index.md)

Configuration for the Digital Credential API (DCAPI).

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)<br>Builder for [DCAPIConfig](index.md). |

## Properties

| Name | Summary |
|---|---|
| [enabled](enabled.md) | [androidJvm]<br>val [enabled](enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>whether the DCAPI is enabled, defaults to false. |
| [privilegedAllowlist](privileged-allowlist.md) | [androidJvm]<br>val [privilegedAllowlist](privileged-allowlist.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>the privileged allowlist for the DCAPI, if not set, a default value will be used, see file in assets/privilegedUserAgents.json |
