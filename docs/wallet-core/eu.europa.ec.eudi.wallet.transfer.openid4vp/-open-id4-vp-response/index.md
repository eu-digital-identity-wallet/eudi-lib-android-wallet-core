//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpResponse](index.md)

# OpenId4VpResponse

sealed interface [OpenId4VpResponse](index.md) : Response

#### Inheritors

|                                             |
|---------------------------------------------|
| [DeviceResponse](-device-response/index.md) |

## Types

| Name                                        | Summary                                                                                                                                                                                                                                                                                                                                                                        |
|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DeviceResponse](-device-response/index.md) | [androidJvm]<br>data class [DeviceResponse](-device-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus, val vpToken: VpToken.MsoMdoc, val responseBytes: DeviceResponseBytes, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [OpenId4VpResponse](index.md) |

## Properties

| Name                                                | Summary                                                                                                 |
|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| [consensus](consensus.md)                           | [androidJvm]<br>abstract val [consensus](consensus.md): Consensus.PositiveConsensus                     |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>abstract val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject |
