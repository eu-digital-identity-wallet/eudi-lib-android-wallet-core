//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withDocumentStatusResolver](with-document-status-resolver.md)

# withDocumentStatusResolver

[androidJvm]\
fun [withDocumentStatusResolver](with-document-status-resolver.md)(documentStatusResolver: [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md)): &lt;Error class: unknown class&gt;

Configure with the given [DocumentStatusResolver](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver/index.md) to use for resolving the status of documents. If not set, the default document status resolver will be used which is [eu.europa.ec.eudi.wallet.statium.DocumentStatusResolverImpl](../../../eu.europa.ec.eudi.wallet.statium/-document-status-resolver-impl/index.md) that uses the HttpClient provided in the configuration.

#### Return

this [Builder](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| documentStatusResolver | the document status resolver |
