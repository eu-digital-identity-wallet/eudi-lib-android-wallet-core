//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[getDefaultCreateDocumentSettings](get-default-create-document-settings.md)

# getDefaultCreateDocumentSettings

[androidJvm]\

@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name =
&quot;getDefaultCreateDocumentSettings&quot;)

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)

fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultCreateDocumentSettings](get-default-create-document-settings.md)(
secureAreaIdentifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? =
null,
attestationChallenge: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)? =
null, configure: AndroidKeystoreCreateKeySettings.Builder.()
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null):
CreateDocumentSettings

Returns the default CreateDocumentSettings for
the [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance. The default
settings are based on the EudiWalletConfig and the first available AndroidKeystoreSecureArea
implementation. The [attestationChallenge](get-default-create-document-settings.md) is generated
using
a [SecureRandom](https://developer.android.com/reference/kotlin/java/security/SecureRandom.html)
instance. The [configure](get-default-create-document-settings.md) lambda can be used to further
customize the AndroidKeystoreCreateKeySettings.
If [secureAreaIdentifier](get-default-create-document-settings.md) is not provided, the first
available AndroidKeystoreSecureArea implementation is used.

#### Receiver

the [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance

#### Parameters

androidJvm

|                      |                                                                                     |
|----------------------|-------------------------------------------------------------------------------------|
| secureAreaIdentifier | the AndroidKeystoreSecureArea.identifier where the document's keys should be stored |
| attestationChallenge | the attestation challenge to use when creating the keys                             |
| configure            | a lambda to further customize the AndroidKeystoreCreateKeySettings                  |

#### See also

|                                          |
|------------------------------------------|
| AndroidKeystoreCreateKeySettings.Builder |
| AndroidKeystoreCreateKeySettings         |
| AndroidKeystoreSecureArea                |
| CreateDocumentSettings                   |

#### Throws

|                                                                                                                     |                                                             |
|---------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| [NoSuchElementException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-no-such-element-exception/index.html) | if no AndroidKeystoreSecureArea implementation is available |
