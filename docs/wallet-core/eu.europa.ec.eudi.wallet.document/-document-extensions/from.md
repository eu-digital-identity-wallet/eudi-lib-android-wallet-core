//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentExtensions](index.md)/[from](from.md)

# from

[androidJvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)

@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-name/index.html)(name = &quot;getCredentialPolicy&quot;)

fun CreateDocumentSettings.CredentialPolicy.Companion.[from](from.md)(isoPolicy: MsoMdocPolicy?): CreateDocumentSettings.CredentialPolicy

Converts an MsoMdocPolicy to a CreateDocumentSettings.CredentialPolicy. If the [isoPolicy](from.md) indicates one-time use, OneTimeUse is returned. Otherwise, RotateUse is returned.

#### Receiver

The companion object of CreateDocumentSettings.CredentialPolicy.

#### Return

The corresponding CreateDocumentSettings.CredentialPolicy.

#### Parameters

androidJvm

| | |
|---|---|
| isoPolicy | The MsoMdocPolicy to convert. Can be `null`. |
