//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[storeIssuedDocument](store-issued-document.md)

# storeIssuedDocument

[androidJvm]\
fun [storeIssuedDocument](store-issued-document.md)(request: UnsignedDocument,
data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): StoreDocumentResult

Add a document to the wallet

#### Return

AddDocumentResult

#### Parameters

androidJvm

|         |                                          |
|---------|------------------------------------------|
| request | the issuance request                     |
| data    | the document data provided by the issuer |

#### See also

|                             |
|-----------------------------|
| DocumentManager.addDocument |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
