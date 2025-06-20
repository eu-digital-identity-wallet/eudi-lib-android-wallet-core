//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[GenericResponse](index.md)

# GenericResponse

[androidJvm]\
data class [GenericResponse](index.md)(val resolvedRequestObject: ResolvedRequestObject, val consensus: Consensus.PositiveConsensus.VPTokenConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val respondedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](../-responded-document/index.md)&gt;) : [OpenId4VpResponse](../index.md)

Generic response type for OpenID4VP presentations (non-DCQL).

This class represents responses for verifiable presentation exchanges that use presentation exchange formats rather than DCQL, typically with formats like Presentation Exchange or SD-JWT VC.

## Constructors

| | |
|---|---|
| [GenericResponse](-generic-response.md) | [androidJvm]<br>constructor(resolvedRequestObject: ResolvedRequestObject, consensus: Consensus.PositiveConsensus.VPTokenConsensus, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), response: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, respondedDocuments: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](../-responded-document/index.md)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [consensus](consensus.md) | [androidJvm]<br>open override val [consensus](consensus.md): Consensus.PositiveConsensus.VPTokenConsensus<br>The positive consensus containing the verifiable presentation token |
| [documentIds](document-ids.md) | [androidJvm]<br>val [documentIds](document-ids.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;<br>The list of document IDs included in the response, sorted by their index. This is derived from the [respondedDocuments](responded-documents.md) property, filtering for IndexBased documents. |
| [encryptionParameters](../encryption-parameters.md) | [androidJvm]<br>open val [encryptionParameters](../encryption-parameters.md): EncryptionParameters?<br>The encryption parameters for JARM, if required by the relying party. Returns null if encryption is not required. |
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>open override val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The nonce used for MSO mdoc presentations (if applicable) |
| [resolvedRequestObject](resolved-request-object.md) | [androidJvm]<br>open override val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject<br>The original OpenID4VP request that was resolved and processed |
| [respondedDocuments](responded-documents.md) | [androidJvm]<br>open override val [respondedDocuments](responded-documents.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](../-responded-document/index.md)&gt;<br>List of documents included in this response with their metadata |
| [response](response.md) | [androidJvm]<br>val [response](response.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>The list of verifiable presentation strings returned to the relying party |
| [vpContent](../vp-content.md) | [androidJvm]<br>open val [vpContent](../vp-content.md): VpContent<br>The verifiable presentation content extracted from the consensus. |

## Functions

| Name | Summary |
|---|---|
| [debugLog](debug-log.md) | [androidJvm]<br>open override fun [debugLog](debug-log.md)(logger: [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md), tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>Prints detailed debug information about the generic response. |
