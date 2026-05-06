//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionLogBuilder](index.md)

# TransactionLogBuilder

class [TransactionLogBuilder](index.md)(metadataResolver: (Response) -&gt; [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;?)

Builder class for creating and updating [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) objects for presentation events.

This class encapsulates the logic for constructing and modifying transaction logs based on different stages and types of presentation data (e.g., requests, responses, errors). It aims to make the process of log updates more testable and readable.

#### Parameters

release

| | |
|---|---|
| metadataResolver | A function that resolves a list of DocumentIds to their corresponding metadata strings (e.g., JSON representations). |

## Constructors

| | |
|---|---|
| [TransactionLogBuilder](-transaction-log-builder.md) | [release]<br>constructor(metadataResolver: (Response) -&gt; [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;?) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [createEmptyPresentationLog](create-empty-presentation-log.md) | [release]<br>fun [createEmptyPresentationLog](create-empty-presentation-log.md)(): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Creates an initial, empty [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) for a presentation. |
| [withError](with-error.md) | [release]<br>fun [withError](with-error.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the provided [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) to indicate an error occurred. |
| [withRelyingParty](with-relying-party.md) | [release]<br>fun [withRelyingParty](with-relying-party.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), processedRequest: RequestProcessor.ProcessedRequest): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the provided [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) with relying party information extracted from a RequestProcessor.ProcessedRequest. |
| [withRequest](with-request.md) | [release]<br>fun [withRequest](with-request.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), request: Request): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the provided [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) with information from a Request. |
| [withResponse](with-response.md) | [release]<br>fun [withResponse](with-response.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), response: Response, error: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)? = null): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the provided [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) with information from a Response and an optional [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html). |