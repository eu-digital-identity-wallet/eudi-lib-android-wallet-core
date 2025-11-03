//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[DPoPUsage](index.md)

# DPoPUsage

sealed interface [DPoPUsage](index.md)

Defines how Demonstration of Proof of Possession (DPoP) should be used during the OpenID4VCI issuance protocol.

DPoP is a security mechanism that binds access tokens to a specific client by requiring the client to prove possession of a private key. This helps prevent token theft and misuse in OAuth 2.0 and OpenID Connect flows.

#### Inheritors

| |
|---|
| [Disabled](-disabled/index.md) |
| [IfSupported](-if-supported/index.md) |

## Types

| Name | Summary |
|---|---|
| [Disabled](-disabled/index.md) | [androidJvm]<br>data object [Disabled](-disabled/index.md) : [OpenId4VciManager.Config.DPoPUsage](index.md)<br>Disables the use of DPoP completely. |
| [IfSupported](-if-supported/index.md) | [androidJvm]<br>data class [IfSupported](-if-supported/index.md)(val algorithm: Algorithm = Algorithm.ESP256) : [OpenId4VciManager.Config.DPoPUsage](index.md)<br>Enables DPoP if the server supports it, using the specified algorithm. |
