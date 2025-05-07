//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)/[onTransferEvent](on-transfer-event.md)

# onTransferEvent

[androidJvm]\
open override fun [onTransferEvent](on-transfer-event.md)(event: TransferEvent)

Handles transfer events to update the transaction log.

Updates the log based on the event type:

- 
   TransferEvent.Connected: Resets the log for a new transaction.
- 
   TransferEvent.RequestReceived: Updates the log with request and relying party information.
- 
   TransferEvent.Error: Marks the log with an error status and persists it.
- 
   TransferEvent.Disconnected: Calls [logStopped](log-stopped.md) to finalize logging for an incomplete transaction.

#### Parameters

androidJvm

| | |
|---|---|
| event | The transfer event to handle. |
