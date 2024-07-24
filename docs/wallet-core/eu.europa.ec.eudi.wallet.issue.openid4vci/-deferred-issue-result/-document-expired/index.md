//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[DeferredIssueResult](../index.md)/[DocumentExpired](index.md)

# DocumentExpired

[androidJvm]\
data class [DocumentExpired](index.md)(val documentId: DocumentId, val
name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [DeferredIssueResult](../index.md)

Document issuance expired.

## Constructors

|                                         |                                                                                                                                                                                                                                        |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentExpired](-document-expired.md) | [androidJvm]<br>constructor(documentId: DocumentId, name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Properties

| Name                         | Summary                                                                                                                                                         |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [docType](doc-type.md)       | [androidJvm]<br>open override val [docType](doc-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the document type |
| [documentId](document-id.md) | [androidJvm]<br>open override val [documentId](document-id.md): DocumentId<br>the id of the expired document                                                    |
| [name](name.md)              | [androidJvm]<br>open override val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the name of the document |
