//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.logging](../index.md)/[Logger](index.md)

# Logger

[release]\
fun interface [Logger](index.md)

Logger interface for the EudiWallet

Implementors can provide their own logging mechanism by implementing this interface

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md)<br>Companion object for the [Logger](index.md) interface |
| [Level](-level/index.md) | [release]<br>annotation class [Level](-level/index.md)<br>Log level annotation for configuring logging with [EudiWalletConfig.configureLogging](../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/configure-logging.md) |
| [Record](-record/index.md) | [release]<br>data class [Record](-record/index.md)(val level: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html), val instant: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html) = Instant.now(), val message: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val thrown: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)? = null, val sourceClassName: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null, val sourceMethod: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null)<br>Log record |

## Functions

| Name | Summary |
|---|---|
| [log](log.md) | [release]<br>abstract fun [log](log.md)(record: [Logger.Record](-record/index.md))<br>Log a record |