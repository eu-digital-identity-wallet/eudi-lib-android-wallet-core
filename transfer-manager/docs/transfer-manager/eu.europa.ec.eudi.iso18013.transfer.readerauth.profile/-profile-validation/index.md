//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth.profile](../index.md)/[ProfileValidation](index.md)

# ProfileValidation

fun interface [ProfileValidation](index.md)

#### Inheritors

| |
|---|
| [AuthorityKey](../-authority-key/index.md) |
| [CommonName](../-common-name/index.md) |
| [CriticalExtensions](../-critical-extensions/index.md) |
| [KeyExtended](../-key-extended/index.md) |
| [KeyUsage](../-key-usage/index.md) |
| [MandatoryExtensions](../-mandatory-extensions/index.md) |
| [Period](../-period/index.md) |
| [ProfileValidationImpl](../-profile-validation-impl/index.md) |
| [SignatureAlgorithm](../-signature-algorithm/index.md) |
| [SubjectKey](../-subject-key/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [release]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [validate](validate.md) | [release]<br>abstract fun [validate](validate.md)(chain: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;, trustCA: [X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)): [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) |