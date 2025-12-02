//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[ClientAuthenticationType](index.md)

# ClientAuthenticationType

sealed interface [ClientAuthenticationType](index.md)

Client Authentication for the OpenId4Vci issuer

#### Inheritors

| |
|---|
| [None](-none/index.md) |
| [AttestationBased](-attestation-based/index.md) |

## Types

| Name | Summary |
|---|---|
| [AttestationBased](-attestation-based/index.md) | [androidJvm]<br>data object [AttestationBased](-attestation-based/index.md) : [OpenId4VciManager.ClientAuthenticationType](index.md)<br>Attestation based client authentication using [WalletAttestationsProvider](../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md) declared in [eu.europa.ec.eudi.wallet.EudiWallet.Builder](../../../eu.europa.ec.eudi.wallet/-eudi-wallet/-builder/index.md) |
| [None](-none/index.md) | [androidJvm]<br>data class [None](-none/index.md)(val clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [OpenId4VciManager.ClientAuthenticationType](index.md)<br>No client authentication, only client id is provided |
