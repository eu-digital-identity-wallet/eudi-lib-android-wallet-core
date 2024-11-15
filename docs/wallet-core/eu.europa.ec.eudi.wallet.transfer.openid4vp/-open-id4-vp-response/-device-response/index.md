//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[DeviceResponse](index.md)

# DeviceResponse

[androidJvm]\
data class [DeviceResponse](index.md)(val resolvedRequestObject: ResolvedRequestObject, val
consensus: Consensus.PositiveConsensus, val vpToken: VpToken.MsoMdoc, val responseBytes:
DeviceResponseBytes, val
msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [OpenId4VpResponse](../index.md)

## Constructors

|                                       |                                                                                                                                                                                                                                                                                 |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DeviceResponse](-device-response.md) | [androidJvm]<br>constructor(resolvedRequestObject: ResolvedRequestObject, consensus: Consensus.PositiveConsensus, vpToken: VpToken.MsoMdoc, responseBytes: DeviceResponseBytes, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Properties

| Name                                                | Summary                                                                                                                                 |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| [consensus](consensus.md)                           | [androidJvm]<br>open override val [consensus](consensus.md): Consensus.PositiveConsensus                                                |
| [msoMdocNonce](mso-mdoc-nonce.md)                   | [androidJvm]<br>val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>open override val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject                            |
| [responseBytes](response-bytes.md)                  | [androidJvm]<br>val [responseBytes](response-bytes.md): DeviceResponseBytes                                                             |
| [vpToken](vp-token.md)                              | [androidJvm]<br>val [vpToken](vp-token.md): VpToken.MsoMdoc                                                                             |
