//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureLogging](configure-logging.md)

# configureLogging

[androidJvm]\

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)

fun [configureLogging](configure-logging.md)(level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sizeLimit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null): &lt;Error class: unknown class&gt;

Configure the built-in logging. This allows to configure the log level and the log size limit.

The default log level is set to [Logger.LEVEL_INFO](../../eu.europa.ec.eudi.wallet.logging/-logger/-companion/-l-e-v-e-l_-i-n-f-o.md) and the default log size limit is set to

1. 

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| level | the log level |
| sizeLimit | the log size limit |
