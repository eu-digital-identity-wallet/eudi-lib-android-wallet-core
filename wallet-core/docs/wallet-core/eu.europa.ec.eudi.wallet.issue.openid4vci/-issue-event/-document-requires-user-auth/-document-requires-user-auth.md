//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentRequiresUserAuth](index.md)/[DocumentRequiresUserAuth](-document-requires-user-auth.md)

# DocumentRequiresUserAuth

[release]\
constructor(document: UnsignedDocument, signingAlgorithm: Algorithm, keysRequireAuth: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), SecureArea&gt;, resume: (keyUnlockData: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KeyAlias](../../-key-alias/index.md), KeyUnlockData?&gt;) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html), cancel: (reason: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)?) -&gt; [Unit](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-unit/index.html))