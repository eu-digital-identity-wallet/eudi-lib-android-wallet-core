//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../../index.md)/[DPopConfig](../index.md)/[Disabled](index.md)

# Disabled

[androidJvm]\
data object [Disabled](index.md) : [DPopConfig](../index.md)

DPoP is disabled.

When this configuration is used, no DPoP proofs are generated or sent during credential issuance. Access tokens will not be bound to cryptographic keys.

Use this when:

- 
   The authorization server does not support DPoP
- 
   DPoP is not required for your use case
- 
   Testing without DPoP protection
