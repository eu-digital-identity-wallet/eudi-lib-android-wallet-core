//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[ProcessedGenericOpenId4VpRequest](index.md)

# ProcessedGenericOpenId4VpRequest

[androidJvm]\
class [ProcessedGenericOpenId4VpRequest](index.md)(documentManager: DocumentManager, resolvedRequestObject: ResolvedRequestObject, inputDescriptorMap: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;InputDescriptorId, [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;&gt;, requestedDocuments: RequestedDocuments, val msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) : RequestProcessor.ProcessedRequest.Success

## Constructors

| | |
|---|---|
| [ProcessedGenericOpenId4VpRequest](-processed-generic-open-id4-vp-request.md) | [androidJvm]<br>constructor(documentManager: DocumentManager, resolvedRequestObject: ResolvedRequestObject, inputDescriptorMap: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;InputDescriptorId, [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;DocumentId&gt;&gt;, requestedDocuments: RequestedDocuments, msoMdocNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) |

## Properties

| Name | Summary |
|---|---|
| [msoMdocNonce](mso-mdoc-nonce.md) | [androidJvm]<br>val [msoMdocNonce](mso-mdoc-nonce.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? |
| [requestedDocuments](index.md#1436173325%2FProperties%2F1615067946) | [androidJvm]<br>val [requestedDocuments](index.md#1436173325%2FProperties%2F1615067946): RequestedDocuments |

## Functions

| Name | Summary |
|---|---|
| [generateResponse](generate-response.md) | [androidJvm]<br>open override fun [generateResponse](generate-response.md)(disclosedDocuments: DisclosedDocuments, signatureAlgorithm: Algorithm?): ResponseResult |
| [getOrNull](index.md#1268647320%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrNull](index.md#1268647320%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success? |
| [getOrThrow](index.md#-927339947%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getOrThrow](index.md#-927339947%2FFunctions%2F1615067946)(): RequestProcessor.ProcessedRequest.Success |
