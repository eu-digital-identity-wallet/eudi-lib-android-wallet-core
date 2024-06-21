//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[getAllDocuments](get-all-documents.md)

# getAllDocuments

[androidJvm]\
fun [getAllDocuments](get-all-documents.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)
&lt;Document&gt;

Returns the list of all documents

#### Return

the list of Document

#### See also

|                              |
|------------------------------|
| DocumentManager.getDocuments |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) in not firstly initialized via the [init](init.md) method |
