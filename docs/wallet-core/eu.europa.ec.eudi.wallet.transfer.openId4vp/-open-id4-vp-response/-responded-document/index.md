//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpResponse](../index.md)/[RespondedDocument](index.md)

# RespondedDocument

[androidJvm]\
@Serializable

data class [RespondedDocument](index.md)(val documentId: &lt;Error class: unknown class&gt;, val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Represents a document that was included in an OpenID4VP response.

This data class provides metadata about documents that were presented in response to an OpenID4VP request, supporting both index-based and query-based document identification.

## Constructors

| | |
|---|---|
| [RespondedDocument](-responded-document.md) | [androidJvm]<br>constructor(documentId: &lt;Error class: unknown class&gt;, format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [documentId](document-id.md) | [androidJvm]<br>val [documentId](document-id.md): &lt;Error class: unknown class&gt;<br>The unique identifier of the document that was responded. |
| [format](format.md) | [androidJvm]<br>val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The format of the document (e.g., &quot;mso_mdoc&quot;, &quot;sd_jwt_vc&quot;). Indicates the credential format used for this specific document. |
