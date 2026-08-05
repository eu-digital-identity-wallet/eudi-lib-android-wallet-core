//[document-manager](../../index.md)/[eu.europa.ec.eudi.wallet.document](index.md)/[getDocuments](get-documents.md)

# getDocuments

[release]\
inline fun &lt;[T](get-documents.md) : [Document](-document/index.md)&gt; [DocumentManager](-document-manager/index.md).[getDocuments](get-documents.md)(): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[T](get-documents.md)&gt;

DocumentManager Extension function that returns a list of documents of type [T](get-documents.md). If [T](get-documents.md) is [IssuedDocument](-issued-document/index.md), then only [IssuedDocument](-issued-document/index.md) will be returned. If [T](get-documents.md) is [UnsignedDocument](-unsigned-document/index.md), then only [UnsignedDocument](-unsigned-document/index.md) will be returned, excluding [DeferredDocument](-deferred-document/index.md). If [T](get-documents.md) is [DeferredDocument](-deferred-document/index.md), then only [DeferredDocument](-deferred-document/index.md) will be returned.

#### Receiver

DocumentManager

#### Return

List of documents of type [T](get-documents.md)

#### Type Parameters

release

| | |
|---|---|
| T | The type of document to be returned |