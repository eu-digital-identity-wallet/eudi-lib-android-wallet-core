//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpResponse](index.md)

# OpenId4VpResponse

sealed interface [OpenId4VpResponse](index.md) : Response

#### Inheritors

| |
|---|
| [DeviceResponse](-device-response/index.md) |
| [GenericResponse](-generic-response/index.md) |

## Types

| Name | Summary |
|---|---|
| [DeviceResponse](-device-response/index.md) | [androidJvm]<br>data class [DeviceResponse](-device-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus, val vpToken: VpToken, val responseBytes: DeviceResponseBytes, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [OpenId4VpResponse](index.md) |
| [GenericResponse](-generic-response/index.md) | [androidJvm]<br>data class [GenericResponse](-generic-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus, val vpToken: VpToken, val response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;) : [OpenId4VpResponse](index.md) |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>abstract val [consensus](consensus.md): Consensus.PositiveConsensus |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>abstract val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject |
