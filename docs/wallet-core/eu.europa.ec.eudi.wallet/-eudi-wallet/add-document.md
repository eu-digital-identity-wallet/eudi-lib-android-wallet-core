//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[addDocument](add-document.md)

# addDocument

[androidJvm]\
fun [addDocument](add-document.md)(request: IssuanceRequest, data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): AddDocumentResult

Add a document to the wallet

#### Return

AddDocumentResult

#### Parameters

androidJvm

| | |
|---|---|
| request | the issuance request |
| data | the document data provided by the issuer |

#### See also

| |
|---|
| DocumentManager.addDocument |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
