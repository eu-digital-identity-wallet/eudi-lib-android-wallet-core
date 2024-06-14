//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[DocumentRequiresUserAuth](index.md)

# DocumentRequiresUserAuth

[androidJvm]\
abstract class [DocumentRequiresUserAuth](index.md)(val
name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val
cryptoObject: [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)?) : [IssueEvent](../index.md)

The document issuance requires user authentication.

## Constructors

|                                                             |                                                                                                                                                                                                                                                                                                                                                                    |
|-------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DocumentRequiresUserAuth](-document-requires-user-auth.md) | [androidJvm]<br>constructor(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), cryptoObject: [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)?) |

## Functions

| Name                     | Summary                                                                                                                                        |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| [cancel](cancel.md)      | [androidJvm]<br>abstract fun [cancel](cancel.md)()                                                                                             |
| [resume](resume.md)      | [androidJvm]<br>abstract fun [resume](resume.md)()                                                                                             |
| [toString](to-string.md) | [androidJvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name                             | Summary                                                                                                                                                                                                                                   |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [cryptoObject](crypto-object.md) | [androidJvm]<br>val [cryptoObject](crypto-object.md): [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)?<br>the crypto object to use for authentication |
| [docType](doc-type.md)           | [androidJvm]<br>val [docType](doc-type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                                                                                              |
| [name](name.md)                  | [androidJvm]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)                                                                                                                     |
