//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.logging](../../index.md)/[Logger](../index.md)/[Record](index.md)

# Record

[androidJvm]\
data class [Record](index.md)(val level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), val
instant: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html) = Instant.now(), val
message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
thrown: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? = null, val
sourceClassName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val
sourceMethod: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null)

## Constructors

|                      |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Record](-record.md) | [androidJvm]<br>constructor(level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), instant: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html) = Instant.now(), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), thrown: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? = null, sourceClassName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, sourceMethod: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |

## Properties

| Name                                    | Summary                                                                                                                                               |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| [instant](instant.md)                   | [androidJvm]<br>val [instant](instant.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)                           |
| [level](level.md)                       | [androidJvm]<br>val [level](level.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)                                     |
| [message](message.md)                   | [androidJvm]<br>val [message](message.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                           |
| [sourceClassName](source-class-name.md) | [androidJvm]<br>val [sourceClassName](source-class-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [sourceMethod](source-method.md)        | [androidJvm]<br>val [sourceMethod](source-method.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null        |
| [thrown](thrown.md)                     | [androidJvm]<br>val [thrown](thrown.md): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? = null               |
