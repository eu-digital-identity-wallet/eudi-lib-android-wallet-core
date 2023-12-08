//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document.issue](../index.md)/[IssueDocumentResult](index.md)

# IssueDocumentResult

interface [IssueDocumentResult](index.md)

#### Inheritors

|                                                  |
|--------------------------------------------------|
| [Success](-success/index.md)                     |
| [Failure](-failure/index.md)                     |
| [UserAuthRequired](-user-auth-required/index.md) |

## Types

| Name                                             | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Failure](-failure/index.md)                     | [androidJvm]<br>data class [Failure](-failure/index.md)(val error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [IssueDocumentResult](index.md)                                                                                                                                                                                                                                                                                 |
| [Success](-success/index.md)                     | [androidJvm]<br>data class [Success](-success/index.md)(val documentId: DocumentId) : [IssueDocumentResult](index.md)                                                                                                                                                                                                                                                                                                                                                        |
| [UserAuthRequired](-user-auth-required/index.md) | [androidJvm]<br>data class [UserAuthRequired](-user-auth-required/index.md)(val cryptoObject: [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)? = null, _resume: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), _cancel: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [IssueDocumentResult](index.md) |
