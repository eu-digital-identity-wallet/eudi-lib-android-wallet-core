//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[consumingCredential](consuming-credential.md)

# consumingCredential

[release]\
suspend fun &lt;[T](consuming-credential.md)&gt; [consumingCredential](consuming-credential.md)(credentialContext: suspend SecureAreaBoundCredential.() -&gt; [T](consuming-credential.md)): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;[T](consuming-credential.md)&gt;

Performs an operation with a valid credential and handles usage policy enforcement.

This method finds a valid credential, executes the provided block with it, and then applies the appropriate credential policy (either incrementing usage count for RotateUse or deleting the credential for OneTimeUse).

#### Return

A [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html) containing the operation result or an exception if the operation failed

#### Parameters

release

| | |
|---|---|
| credentialContext | The suspend function to execute with the credential as receiver |

#### Type Parameters

release

| | |
|---|---|
| T | The return type of the operation |