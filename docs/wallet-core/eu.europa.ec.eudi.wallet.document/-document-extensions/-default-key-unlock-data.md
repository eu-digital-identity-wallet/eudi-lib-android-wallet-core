//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[DefaultKeyUnlockData](-default-key-unlock-data.md)

# DefaultKeyUnlockData

[androidJvm]\

@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultKeyUnlockData&quot;)

@get:[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

val Document.[DefaultKeyUnlockData](-default-key-unlock-data.md): AndroidKeystoreKeyUnlockData?

Returns the default AndroidKeystoreKeyUnlockData for the Document instance. The default key unlock data is based on the Document.keyAlias.

#### Receiver

the Document instance

#### Return

the default AndroidKeystoreKeyUnlockData for the Document instance if document requires user authentication

#### See also

| |
|---|
| AndroidKeystoreKeyUnlockData |
| Document |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-illegal-state-exception/index.html) | if the Document is not managed by AndroidKeystoreSecureArea |
