//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[DeviceResponse](index.md)

# DeviceResponse

[androidJvm]\
data class [DeviceResponse](index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val sessionTranscript: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html), val responseBytes: &lt;Error class: unknown class&gt;, val respondedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](../-responded-document/index.md)&gt;) : [OpenId4VpResponse](../index.md)

Response type for device-based OpenID4VP presentations (e.g., MSO mdoc).

This class represents responses for ISO 18013-5 mobile driving license (mDL) formatted presentations and other device-based credential disclosures using the MSO mdoc format.

## Constructors

| | |
|---|---|
| [DeviceResponse](-device-response.md) | [androidJvm]<br>constructor(resolvedRequestObject: ResolvedRequestObject, consensus: Consensus.PositiveConsensus.VPTokenConsensus, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), sessionTranscript: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html), responseBytes: &lt;Error class: unknown class&gt;, respondedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](../-responded-document/index.md)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>open override val [consensus](consensus.md): Consensus.PositiveConsensus.VPTokenConsensus<br>The positive consensus containing the verifiable presentation token |
| [documentIds](document-ids.md) | [androidJvm]<br>val [documentIds](document-ids.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;&lt;Error class: unknown class&gt;&gt;<br>The list of document IDs included in the response, sorted by their index. This is derived from the [respondedDocuments](responded-documents.md) property, filtering for IndexBased documents. |
| [encryptionParameters](../encryption-parameters.md) | [androidJvm]<br>open val [encryptionParameters](../encryption-parameters.md): EncryptionParameters?<br>The encryption parameters for JARM, if required by the relying party. Returns null if encryption is not required. |
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>open override val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The nonce used for MSO mdoc presentations to ensure freshness |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>open override val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject<br>The original OpenID4VP request that was resolved and processed |
| [respondedDocuments](responded-documents.md) | [androidJvm]<br>open override val [respondedDocuments](responded-documents.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](../-responded-document/index.md)&gt;<br>List of documents included in this response with their metadata |
| [responseBytes](response-bytes.md) | [androidJvm]<br>val [responseBytes](response-bytes.md): &lt;Error class: unknown class&gt;<br>The raw CBOR-encoded device response bytes to be transmitted to the verifier |
| [sessionTranscript](session-transcript.md) | [androidJvm]<br>val [sessionTranscript](session-transcript.md): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte-array/index.html)<br>The session transcript bytes used in the device response for binding the presentation |
| [vpContent](../vp-content.md) | [androidJvm]<br>open val [vpContent](../vp-content.md): VpContent<br>The verifiable presentation content extracted from the consensus. |

## Functions

| Name | Summary |
|---|---|
| [debugLog](debug-log.md) | [androidJvm]<br>open override fun [debugLog](debug-log.md)(logger: [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md), tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>Prints detailed debug information about the device response. |
| [equals](equals.md) | [androidJvm]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [androidJvm]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
