//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionLogBuilder](index.md)/[withRelyingParty](with-relying-party.md)

# withRelyingParty

[androidJvm]\
fun [withRelyingParty](with-relying-party.md)(log: [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md), processedRequest: RequestProcessor.ProcessedRequest): [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md)

Updates the provided [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) with relying party information extracted from a RequestProcessor.ProcessedRequest.

If the log's type is not [TransactionLog.Type.Presentation](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-type/-presentation/index.md), it returns the log unchanged. It extracts the relying party's name, certificate chain (Base64 encoded), verification status, and reader authentication data (Base64 encoded) if available. If reader authentication details are not found, a default name is used.

The timestamp of the log is updated to the current time.

#### Return

An updated [TransactionLog](../../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/index.md) instance.

#### Parameters

androidJvm

| | |
|---|---|
| log | The current transaction log to update. |
| processedRequest | The processed request containing relying party details. |
