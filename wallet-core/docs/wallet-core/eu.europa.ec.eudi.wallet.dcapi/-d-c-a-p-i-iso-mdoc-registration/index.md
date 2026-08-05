//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.dcapi](../index.md)/[DCAPIIsoMdocRegistration](index.md)

# DCAPIIsoMdocRegistration

[release]\
class [DCAPIIsoMdocRegistration](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : [DCAPIRegistration](../-d-c-a-p-i-registration/index.md)

[DCAPIIsoMdocRegistration](index.md) is responsible for registering MSO MDOC credentials for the Digital Credential API (DCAPI).

It collects all issued mdoc documents from the DocumentManager and hands them to an IsoMdocRegistry (a DigitalCredentialRegistry subclass implementing the `org-iso-mdoc` protocol per ISO/IEC TS 18013-7:2025 Annex C) which is then registered with the system [RegistryManager](https://developer.android.com/reference/kotlin/androidx/credentials/registry/provider/RegistryManager.html).

## Constructors

| | |
|---|---|
| [DCAPIIsoMdocRegistration](-d-c-a-p-i-iso-mdoc-registration.md) | [release]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [registerCredentials](register-credentials.md) | [release]<br>open suspend override fun [registerCredentials](register-credentials.md)() |
| [unregisterCredentials](unregister-credentials.md) | [release]<br>open suspend override fun [unregisterCredentials](unregister-credentials.md)() |