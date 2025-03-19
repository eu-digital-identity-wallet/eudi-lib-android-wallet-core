//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withDocumentManager](with-document-manager.md)

# withDocumentManager

[androidJvm]\
fun [withDocumentManager](with-document-manager.md)(documentManager: DocumentManager): &lt;Error class: unknown class&gt;

Configure with the given DocumentManager to use. If not set, the default document manager will be used which is DocumentManagerImpl configured with the provided [storageEngine](storage-engine.md) and [secureAreas](secure-areas.md) if they are set.

#### Return

this [Builder](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| documentManager | the document manager |
