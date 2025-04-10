//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing](index.md)/[parseMsoMdocFromVp](parse-mso-mdoc-from-vp.md)

# parseMsoMdocFromVp

[androidJvm]\
fun [parseMsoMdocFromVp](parse-mso-mdoc-from-vp.md)(vp: VerifiablePresentation.Generic, metadata: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?&gt;?): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PresentedDocument](../eu.europa.ec.eudi.wallet.transactionLogging.presentation/-presented-document/index.md)&gt;

Parses an mso_mdoc document from a Verifiable Presentation. This function extracts the mso_mdoc data from the verifiable presentation, decodes it from Base64, and then passes it to the parseMsoMdoc function.

#### Return

A list of PresentedDocument objects parsed from the mso_mdoc data.

#### Parameters

androidJvm

| | |
|---|---|
| vp | The generic Verifiable Presentation containing the mso_mdoc document. |
| metadata | Optional list of metadata strings associated with the documents. |
