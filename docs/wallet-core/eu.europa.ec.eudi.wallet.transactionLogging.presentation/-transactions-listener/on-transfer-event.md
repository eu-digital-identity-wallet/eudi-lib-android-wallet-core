//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)/[onTransferEvent](on-transfer-event.md)

# onTransferEvent

[androidJvm]\
open override fun [onTransferEvent](on-transfer-event.md)(event: TransferEvent)

Listen for transfer events

Updates the log based on the event type.

- 
   TransferEvent.Connected - resets the log
- 
   TransferEvent.RequestReceived - updates the log with the request and relying party
- 
   TransferEvent.Error - updates the log with the error status
