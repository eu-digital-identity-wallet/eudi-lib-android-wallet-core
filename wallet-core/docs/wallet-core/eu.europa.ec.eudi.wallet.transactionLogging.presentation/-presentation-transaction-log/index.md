//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[PresentationTransactionLog](index.md)

# PresentationTransactionLog

[release]\
data class [PresentationTransactionLog](index.md)(val timestamp: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html), val status: [TransactionLog.Status](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/index.md), val relyingParty: [TransactionLog.RelyingParty](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-relying-party/index.md), val documents: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedDocument](../-presented-document/index.md)&gt;)

Data class representing a presentation transaction log.

## Constructors

| | |
|---|---|
| [PresentationTransactionLog](-presentation-transaction-log.md) | [release]<br>constructor(timestamp: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html), status: [TransactionLog.Status](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/index.md), relyingParty: [TransactionLog.RelyingParty](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-relying-party/index.md), documents: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedDocument](../-presented-document/index.md)&gt;) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [documents](documents.md) | [release]<br>val [documents](documents.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedDocument](../-presented-document/index.md)&gt;<br>The list of presented documents. |
| [relyingParty](relying-party.md) | [release]<br>val [relyingParty](relying-party.md): [TransactionLog.RelyingParty](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-relying-party/index.md)<br>The relying party associated with the transaction. |
| [status](status.md) | [release]<br>val [status](status.md): [TransactionLog.Status](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/index.md)<br>The status of the transaction. |
| [timestamp](timestamp.md) | [release]<br>val [timestamp](timestamp.md): [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)<br>The timestamp of the transaction. |