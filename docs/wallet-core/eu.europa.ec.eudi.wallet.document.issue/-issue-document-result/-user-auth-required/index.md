//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.document.issue](../../index.md)/[IssueDocumentResult](../index.md)/[UserAuthRequired](index.md)

# UserAuthRequired

[androidJvm]\
data class [UserAuthRequired](index.md)(val cryptoObject: [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)?, onResume: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), onCancel: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [IssueDocumentResult](../index.md)

The document issuance requires user authentication.

## Constructors

| | |
|---|---|
| [UserAuthRequired](-user-auth-required.md) | [androidJvm]<br>constructor(cryptoObject: [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)?, onResume: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), onCancel: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [androidJvm]<br>fun [cancel](cancel.md)() |
| [resume](resume.md) | [androidJvm]<br>fun [resume](resume.md)() |

## Properties

| Name | Summary |
|---|---|
| [cryptoObject](crypto-object.md) | [androidJvm]<br>val [cryptoObject](crypto-object.md): [BiometricPrompt.CryptoObject](https://developer.android.com/reference/kotlin/androidx/biometric/BiometricPrompt.CryptoObject.html)?<br>the crypto object to use for authentication |
