//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[RespondedDocument](index.md)

# RespondedDocument

@Serializable

sealed interface [RespondedDocument](index.md)

Interface representing a document that was included in an OpenID4VP response.

This can be either index-based (positioned by index in the response) or query-based (associated with a specific query ID).

#### Inheritors

| |
|---|
| [IndexBased](-index-based/index.md) |
| [QueryBased](-query-based/index.md) |

## Types

| Name | Summary |
|---|---|
| [IndexBased](-index-based/index.md) | [androidJvm]<br>@Serializable<br>data class [IndexBased](-index-based/index.md)(val documentId: &lt;Error class: unknown class&gt;, val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) : [OpenId4VpResponse.RespondedDocument](index.md)<br>Index-based representation of a responded document. |
| [QueryBased](-query-based/index.md) | [androidJvm]<br>@Serializable<br>data class [QueryBased](-query-based/index.md)(val documentId: &lt;Error class: unknown class&gt;, val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val queryId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [OpenId4VpResponse.RespondedDocument](index.md)<br>Query-based representation of a responded document. |

## Properties

| Name | Summary |
|---|---|
| [documentId](document-id.md) | [androidJvm]<br>abstract val [documentId](document-id.md): &lt;Error class: unknown class&gt;<br>The identifier of the document that was responded |
| [format](format.md) | [androidJvm]<br>abstract val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The format of the document (e.g., &quot;mso_mdoc&quot;, &quot;sd_jwt_vc&quot;) |
