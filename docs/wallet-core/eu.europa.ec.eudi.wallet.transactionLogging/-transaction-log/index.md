//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../index.md)/[TransactionLog](index.md)

# TransactionLog

[androidJvm]\
data class [TransactionLog](index.md)(val timestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val status: [TransactionLog.Status](-status/index.md), val type: [TransactionLog.Type](-type/index.md), val relyingParty: [TransactionLog.RelyingParty](-relying-party/index.md)?, val rawRequest: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, val rawResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, val dataFormat: [TransactionLog.DataFormat](-data-format/index.md)?, val sessionTranscript: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, val metadata: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;?)

Represents a transaction log entry.

## Constructors

| | |
|---|---|
| [TransactionLog](-transaction-log.md) | [androidJvm]<br>constructor(timestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), status: [TransactionLog.Status](-status/index.md), type: [TransactionLog.Type](-type/index.md), relyingParty: [TransactionLog.RelyingParty](-relying-party/index.md)?, rawRequest: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, rawResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, dataFormat: [TransactionLog.DataFormat](-data-format/index.md)?, sessionTranscript: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?, metadata: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;?) |

## Types

| Name | Summary |
|---|---|
| [DataFormat](-data-format/index.md) | [androidJvm]<br>enum [DataFormat](-data-format/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.DataFormat](-data-format/index.md)&gt; <br>Represents the format of the data in the [eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.rawResponse](raw-response.md) |
| [RelyingParty](-relying-party/index.md) | [androidJvm]<br>data class [RelyingParty](-relying-party/index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val isVerified: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val certificateChain: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val readerAuth: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?)<br>Represents the relying party involved in the transaction. |
| [Status](-status/index.md) | [androidJvm]<br>enum [Status](-status/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.Status](-status/index.md)&gt; <br>Represents the status of the transaction. |
| [Type](-type/index.md) | [androidJvm]<br>enum [Type](-type/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.Type](-type/index.md)&gt; <br>Represents the type of the transaction. |

## Properties

| Name | Summary |
|---|---|
| [dataFormat](data-format.md) | [androidJvm]<br>val [dataFormat](data-format.md): [TransactionLog.DataFormat](-data-format/index.md)?<br>The format of the data (e.g., CBOR, JSON). |
| [metadata](metadata.md) | [androidJvm]<br>val [metadata](metadata.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;?<br>Additional metadata related to the transaction. |
| [rawRequest](raw-request.md) | [androidJvm]<br>val [rawRequest](raw-request.md): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?<br>The raw request data. |
| [rawResponse](raw-response.md) | [androidJvm]<br>val [rawResponse](raw-response.md): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?<br>The raw response data. |
| [relyingParty](relying-party.md) | [androidJvm]<br>val [relyingParty](relying-party.md): [TransactionLog.RelyingParty](-relying-party/index.md)?<br>The relying party involved in the transaction. |
| [sessionTranscript](session-transcript.md) | [androidJvm]<br>val [sessionTranscript](session-transcript.md): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)?<br>The session transcript data. |
| [status](status.md) | [androidJvm]<br>val [status](status.md): [TransactionLog.Status](-status/index.md)<br>The status of the transaction. |
| [timestamp](timestamp.md) | [androidJvm]<br>val [timestamp](timestamp.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The timestamp of the transaction. |
| [type](type.md) | [androidJvm]<br>val [type](type.md): [TransactionLog.Type](-type/index.md)<br>The type of the transaction. |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [androidJvm]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [androidJvm]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
