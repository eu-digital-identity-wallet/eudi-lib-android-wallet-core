//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[UnsignedDocument](index.md)/[getPoPSigners](get-po-p-signers.md)

# getPoPSigners

[release]\
suspend fun [getPoPSigners](get-po-p-signers.md)(): [ProofOfPossessionSigners](../../eu.europa.ec.eudi.wallet.document.credential/-proof-of-possession-signers/index.md)

Creates proof of possession signers for the document credentials.

This method filters all available credentials associated with this document, selects those bound to the secure area, excludes any invalidated keys, and creates appropriate signers that can prove possession of the corresponding private keys. Only credentials that belong to the current document manager are included.

#### Return

A collection of [ProofOfPossessionSigner](../../eu.europa.ec.eudi.wallet.document.credential/-proof-of-possession-signer/index.md) instances that can be used to sign data          during document issuance