//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[RequestedDocumentsByQueryId](index.md)

# RequestedDocumentsByQueryId

[androidJvm]\
typealias [RequestedDocumentsByQueryId](index.md) = [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;QueryId, [RequestedDocumentsByFormat](../-requested-documents-by-format/index.md)&gt;

Type alias for a map that associates DCQL query identifiers with their corresponding documents and formats.

This mapping is crucial for the DCQL document request processing flow as it allows:

- 
   Tracking which documents match each specific credential query in the DCQL request
- 
   Associating the appropriate format information with each query's documents
- 
   Organizing requested documents by the query that requested them

Used by [DcqlRequestProcessor](../-dcql-request-processor/index.md) to organize processed requests and by [ProcessedDcqlRequest](../-processed-dcql-request/index.md) to generate properly formatted responses.
