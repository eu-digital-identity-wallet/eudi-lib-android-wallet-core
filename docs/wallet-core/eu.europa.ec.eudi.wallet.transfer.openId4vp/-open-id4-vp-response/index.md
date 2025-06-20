//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpResponse](index.md)

# OpenId4VpResponse

sealed interface [OpenId4VpResponse](index.md) : Response

Defines the OpenID4VP response types and related properties for wallet-core.

This sealed interface and its implementations represent the possible responses to an OpenID4VP (OpenID for Verifiable Presentations) request, including device responses and generic DCQL responses. It provides access to the resolved request, consensus, nonce, verifiable presentation content, and encryption parameters for JARM (JWT Secured Authorization Response Mode) requirements.

Implementations of this interface are used to return results to relying parties after successful or failed credential presentations.

#### Inheritors

| |
|---|
| [DeviceResponse](-device-response/index.md) |
| [GenericResponse](-generic-response/index.md) |
| [DcqlResponse](-dcql-response/index.md) |

## Types

| Name | Summary |
|---|---|
| [DcqlResponse](-dcql-response/index.md) | [androidJvm]<br>data class [DcqlResponse](-dcql-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val response: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val respondedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](-responded-document/index.md)&gt;) : [OpenId4VpResponse](index.md)<br>Response type for DCQL-based OpenID4VP presentations. |
| [DeviceResponse](-device-response/index.md) | [androidJvm]<br>data class [DeviceResponse](-device-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val sessionTranscript: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html), val responseBytes: DeviceResponseBytes, val respondedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](-responded-document/index.md)&gt;) : [OpenId4VpResponse](index.md)<br>Response type for device-based OpenID4VP presentations (e.g., MSO mdoc). |
| [GenericResponse](-generic-response/index.md) | [androidJvm]<br>data class [GenericResponse](-generic-response/index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val respondedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](-responded-document/index.md)&gt;) : [OpenId4VpResponse](index.md)<br>Generic response type for OpenID4VP presentations (non-DCQL). |
| [RespondedDocument](-responded-document/index.md) | [androidJvm]<br>@Serializable<br>sealed interface [RespondedDocument](-responded-document/index.md)<br>Interface representing a document that was included in an OpenID4VP response. |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>abstract val [consensus](consensus.md): Consensus.PositiveConsensus.VPTokenConsensus<br>The consensus result containing the verifiable presentation token. |
| [encryptionParameters](encryption-parameters.md) | [androidJvm]<br>open val [encryptionParameters](encryption-parameters.md): EncryptionParameters?<br>The encryption parameters for JARM, if required by the relying party. Returns null if encryption is not required. |
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>abstract val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The nonce used for MSO mdoc presentations. |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>abstract val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject<br>The resolved OpenID4VP request object. |
| [respondedDocuments](responded-documents.md) | [androidJvm]<br>abstract val [respondedDocuments](responded-documents.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](-responded-document/index.md)&gt;<br>The list of responded documents. Can either be index-based or query-based. |
| [vpContent](vp-content.md) | [androidJvm]<br>open val [vpContent](vp-content.md): VpContent<br>The verifiable presentation content extracted from the consensus. |

## Functions

| Name | Summary |
|---|---|
| [debugLog](debug-log.md) | [androidJvm]<br>abstract fun [debugLog](debug-log.md)(logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md), tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>Prints detailed debug information about the response. |
