//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[SecureAreaDpopSigner](index.md)/[release](release.md)

# release

[androidJvm]\
open suspend override fun [release](release.md)(signOperation: SignOperation&lt;JWK&gt;?)

Releases resources associated with a signing operation.

This implementation does not require any cleanup. The DPoP key remains in the secure area and can be reused for subsequent signing operations during the credential issuance flow.

This method is called by the OpenID4VCI library when the signing operation is complete, typically after a DPoP proof has been created and sent to the server.

#### Parameters

androidJvm

| | |
|---|---|
| signOperation | The signing operation to release, or null if no operation     is currently held. This parameter is ignored in this implementation. |
