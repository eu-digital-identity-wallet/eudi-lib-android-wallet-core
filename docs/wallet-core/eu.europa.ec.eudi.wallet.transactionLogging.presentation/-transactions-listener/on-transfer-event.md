//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsListener](index.md)/[onTransferEvent](on-transfer-event.md)

# onTransferEvent

[androidJvm]\
open override fun [onTransferEvent](on-transfer-event.md)(event: TransferEvent)

Handle transfer events

Updates the log based on the event type:

- 
   TransferEvent.Connected - resets the log
- 
   TransferEvent.RequestReceived - updates with request and relying party info
- 
   TransferEvent.Error - marks log with error status
