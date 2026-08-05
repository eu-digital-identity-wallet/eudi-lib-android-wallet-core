//[document-manager](../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](index.md)/[fromIssuerProvidedData](from-issuer-provided-data.md)

# fromIssuerProvidedData

[release]\
fun NameSpacedData.Companion.[fromIssuerProvidedData](from-issuer-provided-data.md)(issuerProvidedData: [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)): NameSpacedData

Creates a NameSpacedData object from raw issuer provided data in CBOR format.

This function parses the CBOR-encoded issuer data and organizes it into a structured namespace-based format that can be used by the wallet.

#### Return

A structured NameSpacedData object containing the parsed credential data

#### Parameters

release

| | |
|---|---|
| issuerProvidedData | Raw CBOR-encoded data from the credential issuer |