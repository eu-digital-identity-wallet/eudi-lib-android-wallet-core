//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpResponse](index.md)

# OpenId4VpResponse

[release]\
class [OpenId4VpResponse](index.md)(val resolvedRequestObject: ResolvedRequestObject, val vpToken: Consensus.PositiveConsensus, val msoMdocNonce: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), val respondedDocuments: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](-responded-document/index.md)&gt;&gt;) : Response

Represents an OpenID4VP (OpenID for Verifiable Presentations) response.

This class encapsulates the complete response to an OpenID4VP request, including the resolved request object, consensus result, nonce for MSO mdoc presentations, and the list of documents that were included in the response.

## Constructors

| | |
|---|---|
| [OpenId4VpResponse](-open-id4-vp-response.md) | [release]<br>constructor(resolvedRequestObject: ResolvedRequestObject, vpToken: Consensus.PositiveConsensus, msoMdocNonce: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), respondedDocuments: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](-responded-document/index.md)&gt;&gt;) |

## Types

| Name | Summary |
|---|---|
| [RespondedDocument](-responded-document/index.md) | [release]<br>@Serializable<br>data class [RespondedDocument](-responded-document/index.md)(val documentId: DocumentId, val format: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html))<br>Represents a document that was included in an OpenID4VP response. |

## Properties

| Name | Summary |
|---|---|
| [encryptionParameters](encryption-parameters.md) | [release]<br>val [encryptionParameters](encryption-parameters.md): EncryptionParameters?<br>The encryption parameters for JARM, if required by the relying party. Returns null if encryption is not required. |
| [msoMdocNonce](mso-mdoc-nonce.md) | [release]<br>val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)<br>The nonce used for MSO mdoc presentations |
| [resolvedRequestObject](resolved-request-object.md) | [release]<br>val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject<br>The resolved OpenID4VP request object that was processed |
| [respondedDocuments](responded-documents.md) | [release]<br>val [respondedDocuments](responded-documents.md): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[OpenId4VpResponse.RespondedDocument](-responded-document/index.md)&gt;&gt;<br>The list of responded documents with their metadata |
| [vpToken](vp-token.md) | [release]<br>val [vpToken](vp-token.md): Consensus.PositiveConsensus<br>The consensus result containing the verifiable presentation token |