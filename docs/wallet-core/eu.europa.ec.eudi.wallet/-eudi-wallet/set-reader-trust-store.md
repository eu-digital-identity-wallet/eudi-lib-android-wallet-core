//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[setReaderTrustStore](set-reader-trust-store.md)

# setReaderTrustStore

[androidJvm]\
fun [setReaderTrustStore](set-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): [EudiWallet](index.md)

Sets the reader trust store with the readers' certificates that are trusted by the wallet

#### Return

[EudiWallet](index.md)

#### Parameters

androidJvm

| | |
|---|---|
| readerTrustStore | the reader trust store |

#### See also

| |
|---|
| TransferManager.setReaderTrustStore |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
