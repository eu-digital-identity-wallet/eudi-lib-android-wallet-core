//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[DeviceResponse](index.md)

# DeviceResponse

[androidJvm]\
data class [DeviceResponse](index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val responseBytes: DeviceResponseBytes) : [OpenId4VpResponse](../index.md)

## Constructors

| | |
|---|---|
| [DeviceResponse](-device-response.md) | [androidJvm]<br>constructor(resolvedRequestObject: ResolvedRequestObject, consensus: Consensus.PositiveConsensus.VPTokenConsensus, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), responseBytes: DeviceResponseBytes) |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>open override val [consensus](consensus.md): Consensus.PositiveConsensus.VPTokenConsensus |
| [encryptionParameters](../encryption-parameters.md) | [androidJvm]<br>open val [encryptionParameters](../encryption-parameters.md): EncryptionParameters? |
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>open override val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>open override val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject |
| [responseBytes](response-bytes.md) | [androidJvm]<br>val [responseBytes](response-bytes.md): DeviceResponseBytes |
| [vpContent](../vp-content.md) | [androidJvm]<br>open val [vpContent](../vp-content.md): VpContent |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [androidJvm]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [androidJvm]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
