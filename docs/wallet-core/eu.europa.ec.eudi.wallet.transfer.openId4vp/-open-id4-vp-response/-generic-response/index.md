//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[GenericResponse](index.md)

# GenericResponse

[androidJvm]\
data class [GenericResponse](index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val documentIds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;) : [OpenId4VpResponse](../index.md)

## Constructors

| | |
|---|---|
| [GenericResponse](-generic-response.md) | [androidJvm]<br>constructor(resolvedRequestObject: ResolvedRequestObject, consensus: Consensus.PositiveConsensus.VPTokenConsensus, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, documentIds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;) |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>open override val [consensus](consensus.md): Consensus.PositiveConsensus.VPTokenConsensus |
| [documentIds](document-ids.md) | [androidJvm]<br>val [documentIds](document-ids.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt; |
| [encryptionParameters](../encryption-parameters.md) | [androidJvm]<br>open val [encryptionParameters](../encryption-parameters.md): EncryptionParameters? |
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>open override val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>open override val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject |
| [response](response.md) | [androidJvm]<br>val [response](response.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt; |
| [vpContent](../vp-content.md) | [androidJvm]<br>open val [vpContent](../vp-content.md): VpContent |
