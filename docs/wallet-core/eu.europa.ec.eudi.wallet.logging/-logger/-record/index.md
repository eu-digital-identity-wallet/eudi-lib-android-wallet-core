//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.logging](../../index.md)/[Logger](../index.md)/[Record](index.md)

# Record

[androidJvm]\
data class [Record](index.md)(val level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), val instant: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html) = Instant.now(), val message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val thrown: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null, val sourceClassName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val sourceMethod: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null)

Log record

## Constructors

| | |
|---|---|
| [Record](-record.md) | [androidJvm]<br>constructor(level: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), instant: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html) = Instant.now(), message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), thrown: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null, sourceClassName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, sourceMethod: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null) |

## Properties

| Name | Summary |
|---|---|
| [instant](instant.md) | [androidJvm]<br>val [instant](instant.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)<br>the instant the log was created |
| [level](level.md) | [androidJvm]<br>val [level](level.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>the log level |
| [message](message.md) | [androidJvm]<br>val [message](message.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>the log message |
| [sourceClassName](source-class-name.md) | [androidJvm]<br>val [sourceClassName](source-class-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>the source class name |
| [sourceMethod](source-method.md) | [androidJvm]<br>val [sourceMethod](source-method.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>the source method name |
| [thrown](thrown.md) | [androidJvm]<br>val [thrown](thrown.md): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null<br>the throwable that was thrown |
