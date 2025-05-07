//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionLogBuilder](index.md)

# TransactionLogBuilder

[androidJvm]\
class [TransactionLogBuilder](index.md)(metadataResolver: ([List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;) -&gt; [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;)

Builder class for creating and updating TransactionLog objects Makes log updates more testable and readable

## Constructors

| | |
|---|---|
| [TransactionLogBuilder](-transaction-log-builder.md) | [androidJvm]<br>constructor(metadataResolver: ([List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;) -&gt; [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;) |

## Functions

| Name | Summary |
|---|---|
| [createEmptyPresentationLog](create-empty-presentation-log.md) | [androidJvm]<br>fun [createEmptyPresentationLog](create-empty-presentation-log.md)(): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Creates an initial empty transaction log for presentation |
| [withError](with-error.md) | [androidJvm]<br>fun [withError](with-error.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the transaction log with error status |
| [withRelyingParty](with-relying-party.md) | [androidJvm]<br>fun [withRelyingParty](with-relying-party.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), processedRequest: RequestProcessor.ProcessedRequest): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the transaction log with relying party information |
| [withRequest](with-request.md) | [androidJvm]<br>fun [withRequest](with-request.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), request: Request): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the transaction log with request information |
| [withResponse](with-response.md) | [androidJvm]<br>fun [withResponse](with-response.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), response: Response, error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)<br>Updates the transaction log with response information |
