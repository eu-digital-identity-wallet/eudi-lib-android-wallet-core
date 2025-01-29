//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.logging](../../index.md)/[Logger](../index.md)/[Companion](index.md)

# Companion

[androidJvm]\
object [Companion](index.md)

Companion object for the [Logger](../index.md) interface

## Properties

| Name | Summary |
|---|---|
| [LEVEL_DEBUG](-l-e-v-e-l_-d-e-b-u-g.md) | [androidJvm]<br>const val [LEVEL_DEBUG](-l-e-v-e-l_-d-e-b-u-g.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 3<br>the log level DEBUG (errors, info and debug) |
| [LEVEL_ERROR](-l-e-v-e-l_-e-r-r-o-r.md) | [androidJvm]<br>const val [LEVEL_ERROR](-l-e-v-e-l_-e-r-r-o-r.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 1<br>the log level ERROR (only errors) |
| [LEVEL_INFO](-l-e-v-e-l_-i-n-f-o.md) | [androidJvm]<br>const val [LEVEL_INFO](-l-e-v-e-l_-i-n-f-o.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 2<br>the log level INFO (errors and info) |
| [OFF](-o-f-f.md) | [androidJvm]<br>const val [OFF](-o-f-f.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 0<br>the log level OFF (no logging) |

## Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | [androidJvm]<br>operator fun [invoke](invoke.md)(config: [EudiWalletConfig](../../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/index.md)): [Logger](../index.md)<br>Construct a default instance of [Logger](../index.md) with the provided [EudiWalletConfig](../../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/index.md) |
