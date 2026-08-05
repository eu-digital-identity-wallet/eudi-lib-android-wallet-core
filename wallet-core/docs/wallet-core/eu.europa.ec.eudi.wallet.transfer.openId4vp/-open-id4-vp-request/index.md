//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpRequest](index.md)

# OpenId4VpRequest

[release]\
data class [OpenId4VpRequest](index.md)(val resolvedRequestObject: ResolvedRequestObject) : Request

## Constructors

| | |
|---|---|
| [OpenId4VpRequest](-open-id4-vp-request.md) | [release]<br>constructor(resolvedRequestObject: ResolvedRequestObject) |

## Properties

| Name | Summary |
|---|---|
| [resolvedRequestObject](resolved-request-object.md) | [release]<br>val [resolvedRequestObject](resolved-request-object.md): ResolvedRequestObject |
| [responseEncryptionParameters](response-encryption-parameters.md) | [release]<br>val [responseEncryptionParameters](response-encryption-parameters.md): EncryptionParameters?<br>Computes the encryption parameters required to send a response (Success or Error) if the Verifier requested JARM encryption. |