//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[ClientIdScheme](index.md)

# ClientIdScheme

sealed interface [ClientIdScheme](index.md)

Sealed interface defining the supported client identifier schemes for OpenID4VP verifier authentication.

Client identifier schemes determine how verifiers authenticate themselves to the wallet during the OpenID4VP flow. Each scheme provides different levels of trust and verification mechanisms, from pre-registered trusted verifiers to certificate-based authentication.

## Supported Schemes:

- 
   [**Preregistered**](-preregistered/index.md): Verifiers known and trusted in advance by the wallet
- 
   [**X509SanDns**](-x509-san-dns/index.md): Certificate-based authentication using DNS Subject Alternative Names
- 
   [**X509Hash**](-x509-hash/index.md): Certificate-based authentication using certificate hash verification
- 
   [**RedirectUri**](-redirect-uri/index.md): Authentication based on redirect URI validation

#### Since

1.0.0

#### See also

| |
|---|
| [OpenId4VpConfig](../-open-id4-vp-config/index.md) |

#### Inheritors

| |
|---|
| [Preregistered](-preregistered/index.md) |
| [X509SanDns](-x509-san-dns/index.md) |
| [X509Hash](-x509-hash/index.md) |
| [RedirectUri](-redirect-uri/index.md) |

## Types

| Name | Summary |
|---|---|
| [Preregistered](-preregistered/index.md) | [androidJvm]<br>data class [Preregistered](-preregistered/index.md)(var preregisteredVerifiers: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[PreregisteredVerifier](../-preregistered-verifier/index.md)&gt;) : [ClientIdScheme](index.md)<br>Client identifier scheme for pre-registered verifiers that are known and trusted by the wallet. |
| [RedirectUri](-redirect-uri/index.md) | [androidJvm]<br>data object [RedirectUri](-redirect-uri/index.md) : [ClientIdScheme](index.md)<br>Client identifier scheme using redirect URI validation. |
| [X509Hash](-x509-hash/index.md) | [androidJvm]<br>data object [X509Hash](-x509-hash/index.md) : [ClientIdScheme](index.md)<br>Client identifier scheme using X.509 certificate hash validation. |
| [X509SanDns](-x509-san-dns/index.md) | [androidJvm]<br>data object [X509SanDns](-x509-san-dns/index.md) : [ClientIdScheme](index.md)<br>Client identifier scheme using X.509 certificate validation with DNS Subject Alternative Names. |
