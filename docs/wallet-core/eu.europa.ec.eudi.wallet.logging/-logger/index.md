//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.logging](../index.md)/[Logger](index.md)

# Logger

interface [Logger](index.md)

#### Inheritors

|                                        |
|----------------------------------------|
| [LoggerImpl](../-logger-impl/index.md) |

## Types

| Name                             | Summary                                                                                          |
|----------------------------------|--------------------------------------------------------------------------------------------------|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md)                                          |
| [Level](-level/index.md)         | [androidJvm]<br>annotation class [Level](-level/index.md)<br>Log level for the OpenId4Vci issuer |

## Functions

| Name          | Summary                                                                                                                                                                                                                                                                                                                                                                                                                    |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [d](d.md)     | [androidJvm]<br>open fun [d](d.md)(tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))                                                                                                                                                                                                       |
| [e](e.md)     | [androidJvm]<br>open fun [e](e.md)(tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? = null)                                                                                            |
| [i](i.md)     | [androidJvm]<br>open fun [i](i.md)(tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))                                                                                                                                                                                                       |
| [log](log.md) | [androidJvm]<br>abstract fun [log](log.md)(level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? = null) |

## Properties

| Name              | Summary                                                                                                                    |
|-------------------|----------------------------------------------------------------------------------------------------------------------------|
| [level](level.md) | [androidJvm]<br>abstract val [level](level.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
