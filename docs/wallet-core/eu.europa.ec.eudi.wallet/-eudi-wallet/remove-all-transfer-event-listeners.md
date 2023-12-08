//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)

# removeAllTransferEventListeners

[androidJvm]\
fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [EudiWallet](index.md)

Removes all transfer event listeners.

#### Return

[EudiWallet](index.md)

#### See also

|                                             |
|---------------------------------------------|
| TransferManager.removeTransferEventListener |
| TransferEvent.Listener                      |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
