//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[getDefaultCreateDocumentSettings](get-default-create-document-settings.md)

# getDefaultCreateDocumentSettings

[androidJvm]\

@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getDefaultCreateDocumentSettings&quot;)

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultCreateDocumentSettings](get-default-create-document-settings.md)(attestationChallenge: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)? = null, numberOfCredentials: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 1, credentialPolicy: CreateDocumentSettings.CredentialPolicy = RotateUse, configure: AndroidKeystoreCreateKeySettings.Builder.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)? = null): CreateDocumentSettings

Returns the default CreateDocumentSettings for the [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance. The default settings are based on the [EudiWalletConfig](../../eu.europa.ec.eudi.wallet/-eudi-wallet-config/index.md) and the presence of an available AndroidKeystoreSecureArea implementation. The [attestationChallenge](get-default-create-document-settings.md) is generated using a [SecureRandom](https://developer.android.com/reference/kotlin/java/security/SecureRandom.html) instance if not provided. The [configure](get-default-create-document-settings.md) lambda can be used to further customize the AndroidKeystoreCreateKeySettings.

#### Receiver

The [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance.

#### Return

The default CreateDocumentSettings.

#### Parameters

androidJvm

| | |
|---|---|
| attestationChallenge | The attestation challenge to use when creating the keys. If `null`, a random challenge will be generated. |
| numberOfCredentials | The number of credentials to pre-generate for the document. Defaults to 1. |
| credentialPolicy | The policy for credential usage (OneTimeUse or RotateUse). Defaults to RotateUse. |
| configure | A lambda to further customize the AndroidKeystoreCreateKeySettings. |

#### See also

| |
|---|
| AndroidKeystoreCreateKeySettings.Builder |
| AndroidKeystoreCreateKeySettings |
| AndroidKeystoreSecureArea |
| CreateDocumentSettings |

#### Throws

| | |
|---|---|
| NoSuchElementException | if no AndroidKeystoreSecureArea implementation is available. |

[androidJvm]\

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

suspend fun [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md).[getDefaultCreateDocumentSettings](get-default-create-document-settings.md)(offeredDocument: [Offer.OfferedDocument](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-offer/-offered-document/index.md), attestationChallenge: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)? = null, configure: AndroidKeystoreCreateKeySettings.Builder.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)? = null): CreateDocumentSettings

Returns the default CreateDocumentSettings for the [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance based on an [Offer.OfferedDocument](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-offer/-offered-document/index.md). The settings are derived from the issuer metadata and the document configuration within the offer. The [attestationChallenge](get-default-create-document-settings.md) is generated using a [SecureRandom](https://developer.android.com/reference/kotlin/java/security/SecureRandom.html) instance if not provided. The [configure](get-default-create-document-settings.md) lambda can be used to further customize the AndroidKeystoreCreateKeySettings.

#### Receiver

The [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance.

#### Return

The default CreateDocumentSettings tailored for the offered document.

#### Parameters

androidJvm

| | |
|---|---|
| offeredDocument | The [Offer.OfferedDocument](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-offer/-offered-document/index.md) from which to derive settings. |
| attestationChallenge | The attestation challenge to use when creating the keys. If `null`, a random challenge will be generated. |
| configure | A lambda to further customize the AndroidKeystoreCreateKeySettings. |

#### Throws

| | |
|---|---|
| NoSuchElementException | if no AndroidKeystoreSecureArea implementation is available. |
