//[wallet-core](../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [DPopConfig](-d-pop-config/index.md) | [androidJvm]<br>sealed interface [DPopConfig](-d-pop-config/index.md)<br>Configuration for DPoP (Demonstrating Proof-of-Possession) in OpenID4VCI credential issuance. |
| [DPopSigner](-d-pop-signer/index.md) | [androidJvm]<br>interface [DPopSigner](-d-pop-signer/index.md) : Signer&lt;JWK&gt; <br>Signer interface for DPoP (Demonstrating Proof-of-Possession) in OpenID4VCI flows. |
| [KeyUnlockDataProvider](-key-unlock-data-provider/index.md) | [androidJvm]<br>fun interface [KeyUnlockDataProvider](-key-unlock-data-provider/index.md)<br>Functional interface for providing unlock data for DPoP keys stored in a secure area. |
| [SecureAreaDpopSigner](-secure-area-dpop-signer/index.md) | [androidJvm]<br>class [SecureAreaDpopSigner](-secure-area-dpop-signer/index.md) : [DPopSigner](-d-pop-signer/index.md)<br>Secure area-based implementation of [DPopSigner](-d-pop-signer/index.md) for OpenID4VCI credential issuance. |
