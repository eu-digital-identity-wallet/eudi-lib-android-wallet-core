//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[getDefaultKeyUnlockData](get-default-key-unlock-data.md)

# getDefaultKeyUnlockData

[androidJvm]\

@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultKeyUnlockData&quot;)

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultKeyUnlockData](get-default-key-unlock-data.md)(documentId: DocumentId): AndroidKeystoreKeyUnlockData?

Returns the default AndroidKeystoreKeyUnlockData for the given DocumentId. The default key unlock data is based on the Document.keyAlias.

#### Receiver

the [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance

#### Return

the default AndroidKeystoreKeyUnlockData for the given DocumentId or null if the document requires no user authentication

#### Parameters

androidJvm

| | |
|---|---|
| documentId | the DocumentId of the document |

#### See also

| |
|---|
| AndroidKeystoreKeyUnlockData |
| Document |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | if the Document is not managed by AndroidKeystoreSecureArea |
| NoSuchElementException | if the document is not found by the DocumentId |
