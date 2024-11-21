//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureDocumentKeyCreation](configure-document-key-creation.md)

# configureDocumentKeyCreation

[androidJvm]\
fun [configureDocumentKeyCreation](configure-document-key-creation.md)(
userAuthenticationRequired: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) =
false,
userAuthenticationTimeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) =
0,
useStrongBoxForKeys: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) =
true): [EudiWalletConfig](index.md)

Configure the document key creation. This allows to configure if user authentication is required to
unlock key usage, the user authentication timeout and whether to use the strong box for keys. These
values are used to create the eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
using [eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings](../../eu.europa.ec.eudi.wallet.document/-document-extensions/get-default-create-document-settings.md)
method.

If user authentication is required, the user authentication timeout must be greater than 0.

**Note**: when setting userAuthenticationRequired to true, device must be secured with a PIN,
pattern or password.

**Note**: when setting useStrongBoxForKeys to true, the device must support the StrongBox.

The default values are:

-
userAuthenticationRequired: false
-
userAuthenticationTimeout: 0
-
useStrongBoxForKeys: true if supported by the device

#### Parameters

androidJvm

|                            |                                                                                                        |
|----------------------------|--------------------------------------------------------------------------------------------------------|
| userAuthenticationRequired | whether user authentication is required                                                                |
| userAuthenticationTimeout  | the user authentication timeout. If user authentication is required, this value must be greater than 0 |
| useStrongBoxForKeys        | whether to use the strong box for keys                                                                 |
