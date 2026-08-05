//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[DocumentManagerImpl](index.md)/[DocumentManagerImpl](-document-manager-impl.md)

# DocumentManagerImpl

[release]\
constructor(identifier: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html), storage: Storage, secureAreaRepository: SecureAreaRepository, ktorHttpClientFactory: () -&gt; HttpClient? = null)

Creates a new DocumentManagerImpl with the required dependencies

#### Parameters

release

| | |
|---|---|
| identifier | Unique identifier for this document manager instance |
| storage | Storage implementation for persisting document data |
| secureAreaRepository | Repository for secure key management and cryptographic operations |
| ktorHttpClientFactory | Optional factory method to create HTTP clients |