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
| [DeviceResponse](-device-response/index.md) | [androidJvm]<br>data class [DeviceResponse](-device-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val responseBytes: DeviceResponseBytes) : [OpenId4VpResponse](index.md) |
| [GenericResponse](-generic-response/index.md) | [androidJvm]<br>data class [GenericResponse](-generic-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;) : [OpenId4VpResponse](index.md) |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>abstract val [consensus](consensus.md): Consensus.PositiveConsensus.VPTokenConsensus |
| [encryptionParameters](encryption-parameters.md) | [androidJvm]<br>open val [encryptionParameters](encryption-parameters.md): EncryptionParameters? |
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>abstract val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>abstract val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject |
| [vpContent](vp-content.md) | [androidJvm]<br>open val [vpContent](vp-content.md): VpContent |
