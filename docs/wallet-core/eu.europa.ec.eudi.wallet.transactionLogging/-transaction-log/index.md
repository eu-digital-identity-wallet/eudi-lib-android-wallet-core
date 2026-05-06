//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../index.md)/[TransactionLog](index.md)

# TransactionLog

[release]\
@Serializable

data class [TransactionLog](index.md)(val timestamp: [Long](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-long/index.html), val status: [TransactionLog.Status](-status/index.md), val type: [TransactionLog.Type](-type/index.md), val relyingParty: [TransactionLog.RelyingParty](-relying-party/index.md)?, val rawRequest: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?, val rawResponse: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?, val dataFormat: [TransactionLog.DataFormat](-data-format/index.md)?, val sessionTranscript: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?, val metadata: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;?)

Represents a transaction log entry.

## Constructors

| | |
|---|---|
| [TransactionLog](-transaction-log.md) | [release]<br>constructor(timestamp: [Long](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-long/index.html), status: [TransactionLog.Status](-status/index.md), type: [TransactionLog.Type](-type/index.md), relyingParty: [TransactionLog.RelyingParty](-relying-party/index.md)?, rawRequest: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?, rawResponse: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?, dataFormat: [TransactionLog.DataFormat](-data-format/index.md)?, sessionTranscript: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?, metadata: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;?) |

## Types

| Name | Summary |
|---|---|
| [DataFormat](-data-format/index.md) | [release]<br>@Serializable<br>enum [DataFormat](-data-format/index.md) : [Enum](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.DataFormat](-data-format/index.md)&gt; <br>Represents the format of the data in the [eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.rawResponse](raw-response.md) |
| [Metadata](-metadata/index.md) | [release]<br>@Serializable<br>data class [Metadata](-metadata/index.md)(val issuerMetadata: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?, val format: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val index: [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html), val queryId: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null) |
| [RelyingParty](-relying-party/index.md) | [release]<br>@Serializable<br>data class [RelyingParty](-relying-party/index.md)(val name: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val isVerified: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html), val certificateChain: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;, val readerAuth: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?)<br>Represents the relying party involved in the transaction. |
| [Status](-status/index.md) | [release]<br>@Serializable<br>enum [Status](-status/index.md) : [Enum](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.Status](-status/index.md)&gt; <br>Represents the status of the transaction. |
| [Type](-type/index.md) | [release]<br>@Serializable<br>enum [Type](-type/index.md) : [Enum](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-enum/index.html)&lt;[TransactionLog.Type](-type/index.md)&gt; <br>Represents the type of the transaction. |

## Properties

| Name | Summary |
|---|---|
| [dataFormat](data-format.md) | [release]<br>val [dataFormat](data-format.md): [TransactionLog.DataFormat](-data-format/index.md)?<br>The format of the data (e.g., CBOR, JSON). |
| [metadata](metadata.md) | [release]<br>val [metadata](metadata.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;?<br>Additional metadata related to the transaction each item is a json-encoded [Metadata](-metadata/index.md) object |
| [rawRequest](raw-request.md) | [release]<br>val [rawRequest](raw-request.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?<br>The raw request data. |
| [rawResponse](raw-response.md) | [release]<br>val [rawResponse](raw-response.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?<br>The raw response data. |
| [relyingParty](relying-party.md) | [release]<br>val [relyingParty](relying-party.md): [TransactionLog.RelyingParty](-relying-party/index.md)?<br>The relying party involved in the transaction. |
| [sessionTranscript](session-transcript.md) | [release]<br>val [sessionTranscript](session-transcript.md): [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)?<br>The session transcript data. |
| [status](status.md) | [release]<br>val [status](status.md): [TransactionLog.Status](-status/index.md)<br>The status of the transaction. |
| [timestamp](timestamp.md) | [release]<br>val [timestamp](timestamp.md): [Long](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-long/index.html)<br>The timestamp of the transaction. |
| [type](type.md) | [release]<br>val [type](type.md): [TransactionLog.Type](-type/index.md)<br>The type of the transaction. |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [release]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [release]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-int/index.html) |