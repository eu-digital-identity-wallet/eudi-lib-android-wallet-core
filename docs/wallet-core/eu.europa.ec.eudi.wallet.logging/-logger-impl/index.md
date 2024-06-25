//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.logging](../index.md)/[LoggerImpl](index.md)

# LoggerImpl

[androidJvm]\
class [LoggerImpl](index.md)(val
config: [EudiWalletConfig](../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/index.md),
maxLogSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) =
1000) : [Logger](../-logger/index.md)

## Constructors

|                               |                                                                                                                                                                                                                     |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [LoggerImpl](-logger-impl.md) | [androidJvm]<br>constructor(config: [EudiWalletConfig](../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/index.md), maxLogSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 1000) |

## Functions

| Name                 | Summary                                                                                                                                                                                                                                                                                                                                                                                                                  |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [d](../-logger/d.md) | [androidJvm]<br>open fun [d](../-logger/d.md)(tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))                                                                                                                                                                                          |
| [e](../-logger/e.md) | [androidJvm]<br>open fun [e](../-logger/e.md)(tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? = null)                                                                               |
| [i](../-logger/i.md) | [androidJvm]<br>open fun [i](../-logger/i.md)(tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))                                                                                                                                                                                          |
| [log](log.md)        | [androidJvm]<br>open override fun [log](log.md)(level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?) |

## Properties

| Name                | Summary                                                                                                                         |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------|
| [config](config.md) | [androidJvm]<br>val [config](config.md): [EudiWalletConfig](../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/index.md)        |
| [level](level.md)   | [androidJvm]<br>open override val [level](level.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
