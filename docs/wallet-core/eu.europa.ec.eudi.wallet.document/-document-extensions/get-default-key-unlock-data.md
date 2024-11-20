//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[getDefaultKeyUnlockData](get-default-key-unlock-data.md)

# getDefaultKeyUnlockData

[androidJvm]\
fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultKeyUnlockData](get-default-key-unlock-data.md)(
documentId: DocumentId): AndroidKeystoreKeyUnlockData?

Returns the default AndroidKeystoreKeyUnlockData for the given DocumentId. The default key unlock
data is based on the Document.keyAlias.

#### Receiver

the [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance

#### Return

the default AndroidKeystoreKeyUnlockData for the given DocumentId or null if the document is not
found

#### Parameters

androidJvm

|            |                                |
|------------|--------------------------------|
| documentId | the DocumentId of the document |

#### See also

|                              |
|------------------------------|
| AndroidKeystoreKeyUnlockData |
| Document                     |
