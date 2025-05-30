//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[DefaultKeyUnlockData](-default-key-unlock-data.md)

# DefaultKeyUnlockData

[androidJvm]\

@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultKeyUnlockData&quot;)

@get:[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

val Document.[~~DefaultKeyUnlockData~~](-default-key-unlock-data.md): AndroidKeystoreKeyUnlockData?

---

### Deprecated

Use getDefaultKeyUnlockData(document) instead

---

Returns the default AndroidKeystoreKeyUnlockData for the Document instance. The default key unlock data is based on the Document.keyAlias. This is applicable only if the document's key requires user authentication.

#### Receiver

the Document instance.

#### Return

The default AndroidKeystoreKeyUnlockData for the Document instance if document requires user authentication, otherwise `null`.

#### See also

| |
|---|
| AndroidKeystoreKeyUnlockData |
| Document |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://developer.android.com/reference/kotlin/java/lang/IllegalStateException.html) | if the Document is not managed by AndroidKeystoreSecureArea. |
