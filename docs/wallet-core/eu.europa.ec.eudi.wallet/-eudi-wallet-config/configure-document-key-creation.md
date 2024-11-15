//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureDocumentKeyCreation](configure-document-key-creation.md)

# configureDocumentKeyCreation

[androidJvm]\
fun [configureDocumentKeyCreation](configure-document-key-creation.md)(
userAuthenticationRequired: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html),
userAuthenticationTimeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html),
useStrongBoxForKeys: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [EudiWalletConfig](index.md)

Configure the document key creation. This allows to configure if user authentication is required to
unlock key usage, the user authentication timeout and whether to use the strong box for keys. These
values are used to create the eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
using [eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings](../../eu.europa.ec.eudi.wallet.document/-document-extensions/get-default-create-document-settings.md)
method.

The default values are:

-
userAuthenticationRequired: false
-
userAuthenticationTimeout: 0
-
useStrongBoxForKeys: true

#### Parameters

androidJvm

|                            |                                         |
|----------------------------|-----------------------------------------|
| userAuthenticationRequired | whether user authentication is required |
| userAuthenticationTimeout  | the user authentication timeout         |
| useStrongBoxForKeys        | whether to use the strong box for keys  |
