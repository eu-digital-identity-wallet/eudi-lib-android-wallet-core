//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[ClientIdScheme](../index.md)/[Preregistered](index.md)

# Preregistered

data class [Preregistered](index.md)(var preregisteredVerifiers: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PreregisteredVerifier](../../-preregistered-verifier/index.md)&gt;) : [ClientIdScheme](../index.md)

Client identifier scheme for pre-registered verifiers that are known and trusted by the wallet.

This scheme allows wallets to maintain a list of trusted verifiers that have been vetted and approved in advance. It provides the highest level of trust as verifiers are explicitly whitelisted with their credentials and metadata.

#### See also

| |
|---|
| [PreregisteredVerifier](../../-preregistered-verifier/index.md) |

## Constructors

| | |
|---|---|
| [Preregistered](-preregistered.md) | [androidJvm]<br>constructor(preregisteredVerifiers: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PreregisteredVerifier](../../-preregistered-verifier/index.md)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [preregisteredVerifiers](preregistered-verifiers.md) | [androidJvm]<br>var [preregisteredVerifiers](preregistered-verifiers.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PreregisteredVerifier](../../-preregistered-verifier/index.md)&gt;<br>List of pre-approved verifier configurations with their client IDs, legal names, API endpoints, and cryptographic parameters |
