//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging](../../index.md)/[TransactionLog](../index.md)/[RelyingParty](index.md)

# RelyingParty

[androidJvm]\
@Serializable

data class [RelyingParty](index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val isVerified: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val certificateChain: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val readerAuth: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?)

Represents the relying party involved in the transaction.

Appears in the context of [Type.Presentation](../-type/-presentation/index.md) and contains information about the relying party.

## Constructors

| | |
|---|---|
| [RelyingParty](-relying-party.md) | [androidJvm]<br>constructor(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), isVerified: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), certificateChain: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, readerAuth: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) |

## Properties

| Name | Summary |
|---|---|
| [certificateChain](certificate-chain.md) | [androidJvm]<br>val [certificateChain](certificate-chain.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>The certificate chain of the relying party. |
| [isVerified](is-verified.md) | [androidJvm]<br>val [isVerified](is-verified.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Indicates whether the relying party is verified. |
| [name](name.md) | [androidJvm]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The name of the relying party. |
| [readerAuth](reader-auth.md) | [androidJvm]<br>val [readerAuth](reader-auth.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?<br>The reader authentication data. |
