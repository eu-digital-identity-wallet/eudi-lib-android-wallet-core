//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[storeIssuedDocument](store-issued-document.md)

# storeIssuedDocument

[androidJvm]\
fun [storeIssuedDocument](store-issued-document.md)(unsignedDocument: UnsignedDocument,
data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): StoreDocumentResult

Add a document to the wallet

#### Return

StoreDocumentResult

#### Parameters

androidJvm

|                  |                                          |
|------------------|------------------------------------------|
| unsignedDocument | the issuance request                     |
| data             | the document data provided by the issuer |

#### See also

|                                     |
|-------------------------------------|
| DocumentManager.storeIssuedDocument |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
