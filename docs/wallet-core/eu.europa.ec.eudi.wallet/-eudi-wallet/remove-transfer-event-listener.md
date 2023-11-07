//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[removeTransferEventListener](remove-transfer-event-listener.md)

# removeTransferEventListener

[androidJvm]\
fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [EudiWallet](index.md)

Removes a transfer event listener.

#### Return

[EudiWallet](index.md)

#### Parameters

androidJvm

| |
|---|
| listener |

#### See also

| |
|---|
| TransferManager.removeTransferEventListener |
| TransferEvent.Listener |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
