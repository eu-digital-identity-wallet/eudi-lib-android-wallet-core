//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[getPresentedDocumentsFromClaims](get-presented-documents-from-claims.md)

# getPresentedDocumentsFromClaims

[androidJvm]\
fun [getPresentedDocumentsFromClaims](get-presented-documents-from-claims.md)(claims: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, JsonElement?&gt;, metadata: IssuerMetadata?): [PresentedDocument](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presented-document/index.md)

Function to get the presented documents from the claims It takes the claims as a map of path to value and metadata and returns a PresentedDocument object

#### Return

the PresentedDocument object

#### Parameters

androidJvm

| | |
|---|---|
| claims | the claims to parse |
| metadata | the metadata to parse |
