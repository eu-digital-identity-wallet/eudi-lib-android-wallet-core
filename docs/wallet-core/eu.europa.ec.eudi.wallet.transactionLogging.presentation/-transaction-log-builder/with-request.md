//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionLogBuilder](index.md)/[withRequest](with-request.md)

# withRequest

[release]\
fun [withRequest](with-request.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), request: Request): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)

Updates the provided [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) with information from a Request.

If the log's type is not [TransactionLog.Type.Presentation](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-type/-presentation/index.md), it returns the log unchanged. It handles different types of requests:

- 
   DeviceRequest: Stores the raw request bytes.
- 
   [OpenId4VpRequest](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-request/index.md): Extracts and stores the presentation definition or digital credentials query from the resolved request object. Requires the resolved request to be ResolvedRequestObject and
- 
   Other request types: Marks the log status as [TransactionLog.Status.Error](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-status/-error/index.md).

The timestamp of the log is updated to the current time.

#### Return

An updated [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) instance.

#### Parameters

release

| | |
|---|---|
| log | The current transaction log to update. |
| request | The request object containing data to add to the log. |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if an [OpenId4VpRequest](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-request/index.md) does not conform to expected subtypes. |