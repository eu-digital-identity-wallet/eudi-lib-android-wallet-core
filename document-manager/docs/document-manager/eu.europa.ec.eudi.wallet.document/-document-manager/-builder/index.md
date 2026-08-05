//[document-manager](../../../../index.md)/[eu.europa.ec.eudi.wallet.document](../../index.md)/[DocumentManager](../index.md)/[Builder](index.md)

# Builder

[release]\
class [Builder](index.md)

Builder class to create a [DocumentManager](../index.md) instance.

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [identifier](identifier.md) | [release]<br>var [identifier](identifier.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?<br>the identifier of the document manager |
| [secureAreaRepository](secure-area-repository.md) | [release]<br>var [secureAreaRepository](secure-area-repository.md): SecureAreaRepository?<br>the secure area repository |
| [storage](storage.md) | [release]<br>var [storage](storage.md): Storage?<br>the storage to use for storing/retrieving documents |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [DocumentManager](../index.md)<br>Build a [DocumentManager](../index.md) instance. |
| [setIdentifier](set-identifier.md) | [release]<br>fun [setIdentifier](set-identifier.md)(identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): [DocumentManager.Builder](index.md)<br>Set the identifier of the document manager. |
| [setSecureAreaRepository](set-secure-area-repository.md) | [release]<br>fun [setSecureAreaRepository](set-secure-area-repository.md)(secureAreaRepository: SecureAreaRepository): [DocumentManager.Builder](index.md)<br>Sets the [secureAreaRepository](set-secure-area-repository.md) that will be used for documents' keys management |
| [setStorage](set-storage.md) | [release]<br>fun [setStorage](set-storage.md)(storage: Storage): [DocumentManager.Builder](index.md)<br>Set the storage to use for storing/retrieving documents. |