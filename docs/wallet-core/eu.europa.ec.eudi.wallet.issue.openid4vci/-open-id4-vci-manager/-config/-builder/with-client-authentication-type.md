//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)/[withClientAuthenticationType](with-client-authentication-type.md)

# withClientAuthenticationType

[androidJvm]\
fun [withClientAuthenticationType](with-client-authentication-type.md)(clientAuthenticationType: [OpenId4VciManager.ClientAuthenticationType](../../-client-authentication-type/index.md)): &lt;Error class: unknown class&gt;

Set the client authentication type

Can be either:

- 
   [ClientAuthenticationType.None](../../-client-authentication-type/-none/index.md) provided a client id
- 
   [ClientAuthenticationType.AttestationBased](../../-client-authentication-type/-attestation-based/index.md) using [WalletAttestationsProvider](../../../../eu.europa.ec.eudi.wallet.provider/-wallet-attestations-provider/index.md)

#### Return

this builder

#### Parameters

androidJvm

| | |
|---|---|
| clientAuthenticationType | the client authentication |
