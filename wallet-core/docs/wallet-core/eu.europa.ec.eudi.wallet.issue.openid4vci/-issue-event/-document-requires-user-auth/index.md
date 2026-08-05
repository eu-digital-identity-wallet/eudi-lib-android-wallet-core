//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentRequiresUserAuth](index.md)

# DocumentRequiresUserAuth

[release]\
data class [DocumentRequiresUserAuth](index.md)(val document: UnsignedDocument, val signingAlgorithm: Algorithm, val keysRequireAuth: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), SecureArea&gt;, val resume: (keyUnlockData: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), KeyUnlockData?&gt;) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html), val cancel: (reason: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)) : [IssueEvent](../index.md), DocumentDetails

Document requires user authentication to unlock the key for signing the proof of possession.

## Constructors

| | |
|---|---|
| [DocumentRequiresUserAuth](-document-requires-user-auth.md) | [release]<br>constructor(document: UnsignedDocument, signingAlgorithm: Algorithm, keysRequireAuth: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), SecureArea&gt;, resume: (keyUnlockData: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), KeyUnlockData?&gt;) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html), cancel: (reason: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [release]<br>val [cancel](cancel.md): (reason: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)<br>the callback to cancel the issuance with an optional reason |
| [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F-946843593) | [release]<br>open override val [docType](../-document-deferred/index.md#-1539120442%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [document](document.md) | [release]<br>val [document](document.md): UnsignedDocument<br>the document that requires user authentication |
| [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F-946843593) | [release]<br>open override val [documentId](../-document-deferred/index.md#-811584596%2FProperties%2F-946843593): DocumentId |
| [keysRequireAuth](keys-require-auth.md) | [release]<br>val [keysRequireAuth](keys-require-auth.md): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), SecureArea&gt; |
| [name](../-document-deferred/index.md#686046743%2FProperties%2F-946843593) | [release]<br>open override val [name](../-document-deferred/index.md#686046743%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) |
| [resume](resume.md) | [release]<br>val [resume](resume.md): (keyUnlockData: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), KeyUnlockData?&gt;) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html)<br>the callback to resume the issuance with the KeyUnlockData that will be used to unlock the key |
| [signingAlgorithm](signing-algorithm.md) | [release]<br>val [signingAlgorithm](signing-algorithm.md): Algorithm |