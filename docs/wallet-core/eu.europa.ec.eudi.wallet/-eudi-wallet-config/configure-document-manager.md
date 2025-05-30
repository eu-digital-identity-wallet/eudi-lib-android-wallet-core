//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureDocumentManager](configure-document-manager.md)

# configureDocumentManager

[androidJvm]\

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)

fun [configureDocumentManager](configure-document-manager.md)(storagePath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null): &lt;Error class: unknown class&gt;

Configure the built-in document manager.

Allowing to configure the documents storage path and the document manager identifier. The default document manager identifier is set to [DEFAULT_DOCUMENT_MANAGER_IDENTIFIER](-companion/-d-e-f-a-u-l-t_-d-o-c-u-m-e-n-t_-m-a-n-a-g-e-r_-i-d-e-n-t-i-f-i-e-r.md).

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| storagePath | the documents storage path |
| identifier | the document manager identifier |

#### See also

| |
|---|
| DocumentManagerImpl |
| Storage |
