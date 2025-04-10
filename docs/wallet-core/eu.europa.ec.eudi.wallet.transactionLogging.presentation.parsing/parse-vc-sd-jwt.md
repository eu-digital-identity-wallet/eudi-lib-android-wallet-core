//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[parseVcSdJwt](parse-vc-sd-jwt.md)

# parseVcSdJwt

[androidJvm]\
fun [parseVcSdJwt](parse-vc-sd-jwt.md)(vp: VerifiablePresentation.Generic, metadata: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?): [PresentedDocument](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presented-document/index.md)?

Parses an SD-JWT Verifiable Credential from a Verifiable Presentation. This function extracts the SD-JWT data from the verifiable presentation, processes it to remove key binding due to library limitations, and converts it into a PresentedDocument object with all the relevant claims.

#### Return

A PresentedDocument object if parsing is successful, or null if parsing fails.

#### Parameters

androidJvm

| | |
|---|---|
| vp | The generic Verifiable Presentation containing the SD-JWT VC. |
| metadata | Optional metadata string associated with the document in JSON format. |
