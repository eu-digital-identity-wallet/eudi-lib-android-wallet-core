//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)

# DocumentExtensions

[androidJvm]\
object [DocumentExtensions](index.md)

## Properties

| Name | Summary |
|---|---|
| [DefaultKeyUnlockData](-default-key-unlock-data.md) | [androidJvm]<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultKeyUnlockData&quot;)<br>@get:[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>val Document.[DefaultKeyUnlockData](-default-key-unlock-data.md): AndroidKeystoreKeyUnlockData?<br>Returns the default AndroidKeystoreKeyUnlockData for the Document instance. The default key unlock data is based on the Document.keyAlias. |

## Functions

| Name | Summary |
|---|---|
| [getDefaultCreateDocumentSettings](get-default-create-document-settings.md) | [androidJvm]<br>@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultCreateDocumentSettings&quot;)<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultCreateDocumentSettings](get-default-create-document-settings.md)(attestationChallenge: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)? = null, configure: AndroidKeystoreCreateKeySettings.Builder.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): CreateDocumentSettings<br>Returns the default CreateDocumentSettings for the [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance. The default settings are based on the [EudiWalletConfig](../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/index.md) and the first available AndroidKeystoreSecureArea implementation. The [attestationChallenge](get-default-create-document-settings.md) is generated using a [SecureRandom](https://developer.android.com/reference/kotlin/java/security/SecureRandom.html) instance. The [configure](get-default-create-document-settings.md) lambda can be used to further customize the AndroidKeystoreCreateKeySettings. The first available AndroidKeystoreSecureArea implementation is used. |
| [getDefaultKeyUnlockData](get-default-key-unlock-data.md) | [androidJvm]<br>@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultKeyUnlockData&quot;)<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultKeyUnlockData](get-default-key-unlock-data.md)(documentId: DocumentId): AndroidKeystoreKeyUnlockData?<br>Returns the default AndroidKeystoreKeyUnlockData for the given DocumentId. The default key unlock data is based on the Document.keyAlias. |
