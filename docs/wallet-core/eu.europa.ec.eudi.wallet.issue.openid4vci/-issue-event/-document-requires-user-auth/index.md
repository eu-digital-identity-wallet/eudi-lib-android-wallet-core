//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentRequiresUserAuth](index.md)

# DocumentRequiresUserAuth

[androidJvm]\
data class [DocumentRequiresUserAuth](index.md)(val document: UnsignedDocument, val
signingAlgorithm: Algorithm, val resume: (keyUnlockData: KeyUnlockData)
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), val cancel: (
reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?)
-&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [IssueEvent](../index.md),
DocumentDetails

Document requires user authentication to unlock the key for signing the proof of possession.

## Constructors

|                                                             |                                                                                                                                                                                                                                                                                                                                                                                                         |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentRequiresUserAuth](-document-requires-user-auth.md) | [androidJvm]<br>constructor(document: UnsignedDocument, signingAlgorithm: Algorithm, resume: (keyUnlockData: KeyUnlockData) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), cancel: (reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |

## Properties

| Name                                                                              | Summary                                                                                                                                                                                                                                                                                |
|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [cancel](cancel.md)                                                               | [androidJvm]<br>val [cancel](cancel.md): (reason: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>the callback to cancel the issuance with an optional reason |
| [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F1615067946)   | [androidJvm]<br>open override val [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                                                                    |
| [document](document.md)                                                           | [androidJvm]<br>val [document](document.md): UnsignedDocument<br>the document that requires user authentication                                                                                                                                                                        |
| [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F1615067946) | [androidJvm]<br>open override val [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F1615067946): DocumentId                                                                                                                                                        |
| [name](../-document-deferred/index.md#686046743%2FProperties%2F1615067946)        | [androidJvm]<br>open override val [name](../-document-deferred/index.md#686046743%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                                                                         |
| [resume](resume.md)                                                               | [androidJvm]<br>val [resume](resume.md): (keyUnlockData: KeyUnlockData) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>the callback to resume the issuance with the KeyUnlockData that will be used to unlock the key                           |
| [signingAlgorithm](signing-algorithm.md)                                          | [androidJvm]<br>val [signingAlgorithm](signing-algorithm.md): Algorithm                                                                                                                                                                                                                |
