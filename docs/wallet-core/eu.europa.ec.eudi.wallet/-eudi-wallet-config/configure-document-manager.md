//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureDocumentManager](configure-document-manager.md)

# configureDocumentManager

[androidJvm]\

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)

fun [configureDocumentManager](configure-document-manager.md)(storageDir: [File](https://developer.android.com/reference/kotlin/java/io/File.html), identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, encryptDocuments: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = true): &lt;Error class: unknown class&gt;

Configure the built-in document manager.

Allowing to configure the documents storage directory, the document manager identifier and whether to encrypt documents in storage. The default document manager identifier is set to [DEFAULT_DOCUMENT_MANAGER_IDENTIFIER](-companion/-d-e-f-a-u-l-t_-d-o-c-u-m-e-n-t_-m-a-n-a-g-e-r_-i-d-e-n-t-i-f-i-e-r.md).

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| storageDir | the documents storage directory |
| identifier | the document manager identifier |
| encryptDocuments | whether to encrypt documents in storage |

#### See also

| |
|---|
| DocumentManagerImpl |
| AndroidStorageEngine |
