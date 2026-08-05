//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response.device](../../index.md)/[DeviceRequestProcessor](../index.md)/[RequestedMdocDocument](index.md)

# RequestedMdocDocument

[release]\
data class [RequestedMdocDocument](index.md)(val docType: DocType, val requested: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;NameSpace, [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;ElementIdentifier, [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)&gt;&gt;, val readerAuthentication: () -&gt; [ReaderAuth](../../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth/index.md)?, val zkRequestSystemSpecs: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ZkSystemSpec&gt;? = null)

Parsed requested document.

## Constructors

| | |
|---|---|
| [RequestedMdocDocument](-requested-mdoc-document.md) | [release]<br>constructor(docType: DocType, requested: [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;NameSpace, [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;ElementIdentifier, [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)&gt;&gt;, readerAuthentication: () -&gt; [ReaderAuth](../../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth/index.md)?, zkRequestSystemSpecs: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ZkSystemSpec&gt;? = null) |

## Properties

| Name | Summary |
|---|---|
| [docType](doc-type.md) | [release]<br>val [docType](doc-type.md): DocType<br>the document type |
| [readerAuthentication](reader-authentication.md) | [release]<br>val [readerAuthentication](reader-authentication.md): () -&gt; [ReaderAuth](../../../eu.europa.ec.eudi.iso18013.transfer.response/-reader-auth/index.md)?<br>the reader authentication |
| [requested](requested.md) | [release]<br>val [requested](requested.md): [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;NameSpace, [Map](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;ElementIdentifier, [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)&gt;&gt;<br>the requested elements |
| [zkRequestSystemSpecs](zk-request-system-specs.md) | [release]<br>val [zkRequestSystemSpecs](zk-request-system-specs.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;ZkSystemSpec&gt;?<br>the ZKP system specs requested by the verifier, or null if none |