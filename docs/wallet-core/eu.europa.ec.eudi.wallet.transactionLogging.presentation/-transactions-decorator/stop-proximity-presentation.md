//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)/[stopProximityPresentation](stop-proximity-presentation.md)

# stopProximityPresentation

[androidJvm]\
open override fun [stopProximityPresentation](stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Stops the proximity presentation using the delegate [PresentationManager](../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) and ensures the transaction logging is appropriately finalized by calling [TransactionsListener.logStopped](../-transactions-listener/log-stopped.md).

#### Parameters

androidJvm

| | |
|---|---|
| flags | Flags to control the stopping behavior, passed to the delegate. |
