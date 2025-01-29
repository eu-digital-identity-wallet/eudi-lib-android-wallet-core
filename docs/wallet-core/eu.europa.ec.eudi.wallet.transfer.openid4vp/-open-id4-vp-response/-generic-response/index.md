//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[GenericResponse](index.md)

# GenericResponse

[androidJvm]\
data class [GenericResponse](index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus, val vpToken: VpToken, val response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;) : [OpenId4VpResponse](../index.md)

## Constructors

| | |
|---|---|
| [GenericResponse](-generic-response.md) | [androidJvm]<br>constructor(resolvedRequestObject: ResolvedRequestObject, consensus: Consensus.PositiveConsensus, vpToken: VpToken, response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>open override val [consensus](consensus.md): Consensus.PositiveConsensus |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>open override val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject |
| [response](response.md) | [androidJvm]<br>val [response](response.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt; |
| [vpToken](vp-token.md) | [androidJvm]<br>val [vpToken](vp-token.md): VpToken |
