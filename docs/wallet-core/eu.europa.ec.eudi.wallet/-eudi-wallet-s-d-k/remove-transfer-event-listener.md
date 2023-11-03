//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletSDK](index.md)/[removeTransferEventListener](remove-transfer-event-listener.md)

# removeTransferEventListener

[androidJvm]\
fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [EudiWalletSDK](index.md)

Removes a transfer event listener.

#### Return

[EudiWalletSDK](index.md)

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
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWalletSDK](index.md) is not firstly initialized via the [init](init.md) method |
