//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withDocumentStatusResolver](with-document-status-resolver.md)

# withDocumentStatusResolver

[release]\
fun [withDocumentStatusResolver](with-document-status-resolver.md)(documentStatusResolver: [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md)): [EudiWallet.Builder](index.md)

Configure with the given [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md) to use for resolving the status of documents. If not set, the default document status resolver will be used which is [eu.europa.ec.eudi.wallet.statium.DocumentStatusResolverImpl](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver-impl/index.md) that uses the HttpClient provided in the configuration.

#### Return

this [Builder](index.md) instance

#### Parameters

release

| | |
|---|---|
| documentStatusResolver | the document status resolver |