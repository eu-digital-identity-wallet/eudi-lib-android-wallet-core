//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[build](build.md)

# build

[androidJvm]\
fun [build](build.md)(): [EudiWallet](../index.md)

Build the [EudiWallet](../index.md) instance

#### Return

[EudiWallet](../index.md)

#### Throws

|                                                                                                                  |                                                                                                                                                                  |
|------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWalletConfig.documentsStorageDir](../../-eudi-wallet-config/documents-storage-dir.md) is not set and and the default DocumentManager is going to be used |
