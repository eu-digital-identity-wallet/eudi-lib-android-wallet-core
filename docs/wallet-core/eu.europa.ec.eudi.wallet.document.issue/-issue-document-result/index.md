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

| Name                                             | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Failure](-failure/index.md)                     | [androidJvm]<br>data class [Failure](-failure/index.md)(val error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [IssueDocumentResult](index.md)<br>The document issuance failed.                                                                                                                                                                                                                                                                                                  |
| [Success](-success/index.md)                     | [androidJvm]<br>data class [Success](-success/index.md)(val documentId: DocumentId) : [IssueDocumentResult](index.md)<br>The document was successfully issued.                                                                                                                                                                                                                                                                                                                                                                 |
| [UserAuthRequired](-user-auth-required/index.md) | [androidJvm]<br>data class [UserAuthRequired](-user-auth-required/index.md)(val cryptoObject: [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)?, onResume: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), onCancel: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [IssueDocumentResult](index.md)<br>The document issuance requires user authentication. |
