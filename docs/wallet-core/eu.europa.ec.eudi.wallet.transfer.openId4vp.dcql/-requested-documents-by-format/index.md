//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[RequestedDocumentsByFormat](index.md)

# RequestedDocumentsByFormat

[androidJvm]\
data class [RequestedDocumentsByFormat](index.md)(val format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val requestedDocuments: RequestedDocuments)

Data structure that associates a document format with a collection of requested documents.

This class groups documents by their format (e.g., MSO_MDOC or SD_JWT_VC) to ensure proper handling during the credential presentation flow. Each format requires specific processing for presentation generation.

## Constructors

| | |
|---|---|
| [RequestedDocumentsByFormat](-requested-documents-by-format.md) | [androidJvm]<br>constructor(format: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), requestedDocuments: RequestedDocuments) |

## Properties

| Name | Summary |
|---|---|
| [format](format.md) | [androidJvm]<br>val [format](format.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The string identifier of the document format (e.g., &quot;mso_mdoc&quot;, &quot;sd_jwt_vc&quot;) |
| [requestedDocuments](requested-documents.md) | [androidJvm]<br>val [requestedDocuments](requested-documents.md): RequestedDocuments<br>Collection of documents requested in this format |
