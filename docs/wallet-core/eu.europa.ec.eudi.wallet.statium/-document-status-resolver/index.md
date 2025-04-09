//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[DocumentStatusResolver](index.md)

# DocumentStatusResolver

interface [DocumentStatusResolver](index.md)

Interface for resolving the status of a document

#### Inheritors

| |
|---|
| [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) |
| [EudiWalletImpl](../../eu.europa.ec.eudi.wallet/-eudi-wallet-impl/index.md) |
| [DocumentStatusResolverImpl](../-document-status-resolver-impl/index.md) |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)<br>Builder for [DocumentStatusResolver](index.md) It allows to set the parameters for the resolver it builds a [DocumentStatusResolverImpl](../-document-status-resolver-impl/index.md) |
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [resolveStatus](resolve-status.md) | [androidJvm]<br>abstract suspend fun [resolveStatus](resolve-status.md)(document: IssuedDocument): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;Status&gt;<br>Resolves the status of the given document |
