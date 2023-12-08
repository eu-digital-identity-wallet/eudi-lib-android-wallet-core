//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[addTransferEventListener](add-transfer-event-listener.md)

# addTransferEventListener

[androidJvm]\
fun [addTransferEventListener](add-transfer-event-listener.md)(listener:
TransferEvent.Listener): [EudiWallet](index.md)

Adds a transfer event listener in order to be notified about transfer events

#### Return

[EudiWallet](index.md)

#### Parameters

androidJvm

|          |
|----------|
| listener |

#### See also

|                                          |
|------------------------------------------|
| TransferManager.addTransferEventListener |
| TransferEvent.Listener                   |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
