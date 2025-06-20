//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[parseMsoMdocFromVp](parse-mso-mdoc-from-vp.md)

# parseMsoMdocFromVp

[androidJvm]\
fun [parseMsoMdocFromVp](parse-mso-mdoc-from-vp.md)(vp: VerifiablePresentation.Generic, metadata: [TransactionLog.Metadata](../eu.europa.ec.eudi.wallet.transactionLogging/-transaction-log/-metadata/index.md)): [PresentedDocument](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presented-document/index.md)?

Parses an mso_mdoc document from a Verifiable Presentation. This function extracts the mso_mdoc data from the verifiable presentation, decodes it from Base64, and then passes it to the parseMsoMdoc function.

#### Return

A PresentedDocument objects parsed from the mso_mdoc data.

#### Parameters

androidJvm

| | |
|---|---|
| vp | The generic Verifiable Presentation containing the mso_mdoc document. |
| metadata | List of metadata strings associated with the documents. |
