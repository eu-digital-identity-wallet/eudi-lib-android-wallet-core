//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[findClaimMetadataForSdJwtVc](find-claim-metadata-for-sd-jwt-vc.md)

# findClaimMetadataForSdJwtVc

[androidJvm]\
fun [findClaimMetadataForSdJwtVc](find-claim-metadata-for-sd-jwt-vc.md)(path: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, metadata: DocumentMetaData?): DocumentMetaData.Claim?

Function to find the claim metadata from the path and metadata It takes the path as a list of strings and metadata and iterates DocumentMetaData.claims to find the claim metadata

#### Return

the claim metadata as a DocumentMetaData.Claim object

#### Parameters

androidJvm

| | |
|---|---|
| path | the path to parse |
| metadata | the metadata to parse |
