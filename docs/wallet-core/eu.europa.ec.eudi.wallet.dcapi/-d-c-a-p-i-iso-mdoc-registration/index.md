//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIIsoMdocRegistration](index.md)

# DCAPIIsoMdocRegistration

[androidJvm]\
class [DCAPIIsoMdocRegistration](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : [DCAPIRegistration](../-d-c-a-p-i-registration/index.md)

[DCAPIIsoMdocRegistration](index.md) is responsible for registering MSO MDOC credentials for the Digital Credential API (DCAPI).

It retrieves issued documents, converts them to CBOR format, and registers them with the Identity Credential Manager.

## Constructors

| | |
|---|---|
| [DCAPIIsoMdocRegistration](-d-c-a-p-i-iso-mdoc-registration.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [registerCredentials](register-credentials.md) | [androidJvm]<br>open suspend override fun [registerCredentials](register-credentials.md)() |
