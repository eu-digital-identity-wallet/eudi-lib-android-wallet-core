//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[getDefaultKeyUnlockData](get-default-key-unlock-data.md)

# getDefaultKeyUnlockData

[release]\
suspend fun IssuedDocument.[getDefaultKeyUnlockData](get-default-key-unlock-data.md)(): AndroidKeystoreKeyUnlockData?

Returns the default AndroidKeystoreKeyUnlockData for the IssuedDocument. The default key unlock data is based on the IssuedDocument.findCredential

#### Receiver

The IssuedDocument instance.

#### Return

The default AndroidKeystoreKeyUnlockData for the IssuedDocument if it requires user authentication, otherwise `null`.

#### See also

| |
|---|
| [getDefaultKeyUnlockData](get-default-key-unlock-data.md) |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if the document is not managed by AndroidKeystoreSecureArea. |

[release]\

@[JvmName](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultKeyUnlockDataForDocument&quot;)

suspend fun [getDefaultKeyUnlockData](get-default-key-unlock-data.md)(document: IssuedDocument): AndroidKeystoreKeyUnlockData?

Returns the default AndroidKeystoreKeyUnlockData for the given IssuedDocument. The key unlock data is retrieved based on the document's associated credential.

#### Return

The AndroidKeystoreKeyUnlockData for the document if it requires user authentication, otherwise `null`.

#### Parameters

release

| | |
|---|---|
| document | The IssuedDocument for which to retrieve key unlock data. |

#### See also

| |
|---|
| AndroidKeystoreKeyUnlockData |
| IssuedDocument |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-argument-exception/index.html) | if the document is not managed by AndroidKeystoreSecureArea. |

[release]\

@[JvmName](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultKeyUnlockData&quot;)

@[JvmStatic](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultKeyUnlockData](get-default-key-unlock-data.md)(documentId: DocumentId): AndroidKeystoreKeyUnlockData?

Returns the default AndroidKeystoreKeyUnlockData for the given DocumentId. The default key unlock data is based on the Document.keyAlias of the found document. This is applicable only if the document's key requires user authentication.

#### Receiver

The [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance.

#### Return

The default AndroidKeystoreKeyUnlockData for the given DocumentId if the document requires user authentication, otherwise `null`.

#### Parameters

release

| | |
|---|---|
| documentId | The DocumentId of the document. |

#### See also

| |
|---|
| AndroidKeystoreKeyUnlockData |
| Document |

#### Throws

| | |
|---|---|
| [NoSuchElementException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-no-such-element-exception/index.html) | if the document is not found by the DocumentId. |
| [IllegalStateException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-state-exception/index.html) | if the Document is not managed by AndroidKeystoreSecureArea. |

[release]\
fun [getDefaultKeyUnlockData](get-default-key-unlock-data.md)(secureArea: SecureArea, keyAlias: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): AndroidKeystoreKeyUnlockData?

Returns the default AndroidKeystoreKeyUnlockData for the given SecureArea and [keyAlias](get-default-key-unlock-data.md) if the [secureArea](get-default-key-unlock-data.md) is an instance of AndroidKeystoreSecureArea.

#### Return

The default AndroidKeystoreKeyUnlockData if the [secureArea](get-default-key-unlock-data.md) is an instance of AndroidKeystoreSecureArea, otherwise `null`.

#### Parameters

release

| | |
|---|---|
| secureArea | The SecureArea instance. |
| keyAlias | The key alias. |