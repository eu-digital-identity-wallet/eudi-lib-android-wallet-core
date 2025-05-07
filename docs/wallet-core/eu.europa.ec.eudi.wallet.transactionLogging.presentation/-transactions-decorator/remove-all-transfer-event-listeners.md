//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)/[removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)

# removeAllTransferEventListeners

[androidJvm]\
open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): &lt;Error class: unknown class&gt;

Removes all transfer event listeners from the delegate [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and then re-adds the internal transactionListener.

This ensures that the transactionListener remains active even if other listeners are cleared.

#### Return

This instance of [TransactionsDecorator](index.md).
