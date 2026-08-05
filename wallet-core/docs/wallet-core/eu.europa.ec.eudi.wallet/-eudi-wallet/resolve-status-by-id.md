//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[resolveStatusById](resolve-status-by-id.md)

# resolveStatusById

[release]\
open suspend fun [resolveStatusById](resolve-status-by-id.md)(documentId: DocumentId): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;Status&gt;

Resolve the status of the document with the given [documentId](resolve-status-by-id.md)

This method will return the status of the document if it is an IssuedDocument

#### Return

the status of the document

#### Parameters

release

| | |
|---|---|
| documentId | the document ID |