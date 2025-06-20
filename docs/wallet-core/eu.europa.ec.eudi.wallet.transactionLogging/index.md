//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [TransactionLog](-transaction-log/index.md) | [androidJvm]<br>@Serializable<br>data class [TransactionLog](-transaction-log/index.md)(val timestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val status: [TransactionLog.Status](-transaction-log/-status/index.md), val type: [TransactionLog.Type](-transaction-log/-type/index.md), val relyingParty: [TransactionLog.RelyingParty](-transaction-log/-relying-party/index.md)?, val rawRequest: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, val rawResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, val dataFormat: [TransactionLog.DataFormat](-transaction-log/-data-format/index.md)?, val sessionTranscript: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, val metadata: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;?)<br>Represents a transaction log entry. |
| [TransactionLogger](-transaction-logger/index.md) | [androidJvm]<br>interface [TransactionLogger](-transaction-logger/index.md)<br>A logger for transactions. |
