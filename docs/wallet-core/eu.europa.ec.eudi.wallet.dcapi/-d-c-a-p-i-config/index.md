//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIConfig](index.md)

# DCAPIConfig

[release]\
class [DCAPIConfig](index.md)

Configuration for the Digital Credential API (DCAPI).

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [release]<br>class [Builder](-builder/index.md)<br>Builder for [DCAPIConfig](index.md). |

## Properties

| Name | Summary |
|---|---|
| [enabled](enabled.md) | [release]<br>val [enabled](enabled.md): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)<br>whether the DCAPI is enabled, defaults to false. |
| [privilegedAllowlist](privileged-allowlist.md) | [release]<br>val [privilegedAllowlist](privileged-allowlist.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?<br>the privileged allowlist for the DCAPI, if not set, a default value will be used, see file in assets/privilegedUserAgents.json |