//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManager](index.md)/[getDocuments](get-documents.md)

# getDocuments

[release]\
abstract fun [getDocuments](get-documents.md)(predicate: ([Document](../-document/index.md)) -&gt; [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html)? = null): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Document](../-document/index.md)&gt;

Retrieves all documents managed by this DocumentManager instance.

This method returns a list of all documents that are managed by this DocumentManager (matching the DocumentManager's identifier). An optional predicate can be provided to filter the results based on custom criteria.

#### Return

A list of documents matching the criteria, or an empty list if none found or if an error occurs

#### Parameters

release

| | |
|---|---|
| predicate | Optional filter function that takes a Document and returns a boolean                  indicating whether to include the document in the results |