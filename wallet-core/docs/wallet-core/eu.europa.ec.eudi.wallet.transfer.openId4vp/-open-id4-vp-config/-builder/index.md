//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpConfig](../index.md)/[Builder](index.md)

# Builder

[release]\
class [Builder](index.md)

Builder for constructing [OpenId4VpConfig](../index.md) instances with validation and sensible defaults.

This builder implements a fluent API pattern and provides comprehensive validation to ensure the resulting configuration is valid and consistent. It enforces business rules such as allowing only one instance of certain client ID schemes and ensuring required fields are populated.

## Default Values:

- 
   **Encryption Algorithms**: All supported algorithms from [EncryptionAlgorithm.SUPPORTED_ENCRYPTION_ALGORITHMS](../../-encryption-algorithm/-companion/-s-u-p-p-o-r-t-e-d_-e-n-c-r-y-p-t-i-o-n_-a-l-g-o-r-i-t-h-m-s.md)
- 
   **Encryption Methods**: All supported methods from [EncryptionMethod.SUPPORTED_ENCRYPTION_METHODS](../../-encryption-method/-companion/-s-u-p-p-o-r-t-e-d_-e-n-c-r-y-p-t-i-o-n_-m-e-t-h-o-d-s.md)
- 
   **Schemes**: &quot;mdoc-openid4vp&quot;

## Validation Rules:

- 
   Client ID schemes list cannot be empty and must be initialized
- 
   Maximum one [ClientIdScheme.Preregistered](../../-client-id-scheme/-preregistered/index.md) instance allowed
- 
   Maximum one [ClientIdScheme.X509SanDns](../../-client-id-scheme/-x509-san-dns/index.md) instance allowed
- 
   Schemes list cannot be empty
- 
   Encryption algorithms and methods lists cannot be empty
- 
   Formats list cannot be empty and cannot contain duplicate format types
- 
   SD-JWT VC formats must have non-empty algorithm lists

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [release]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [clientIdSchemes](client-id-schemes.md) | [release]<br>lateinit var [clientIdSchemes](client-id-schemes.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../../-client-id-scheme/index.md)&gt;<br>List of supported client identifier schemes (must be initialized) |
| [encryptionAlgorithms](encryption-algorithms.md) | [release]<br>var [encryptionAlgorithms](encryption-algorithms.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../../-encryption-algorithm/index.md)&gt;<br>List of supported encryption algorithms (defaults to all supported) |
| [encryptionMethods](encryption-methods.md) | [release]<br>var [encryptionMethods](encryption-methods.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../../-encryption-method/index.md)&gt;<br>List of supported encryption methods (defaults to all supported) |
| [formats](formats.md) | [release]<br>lateinit var [formats](formats.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Format](../../-format/index.md)&gt;<br>List of supported credential formats (must be initialized) |
| [schemes](schemes.md) | [release]<br>var [schemes](schemes.md): [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>List of URI schemes for OpenID4VP (defaults to &quot;mdoc-openid4vp&quot;) |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [release]<br>fun [build](build.md)(): [OpenId4VpConfig](../index.md)<br>Builds the [OpenId4VpConfig](../index.md). |
| [withClientIdSchemes](with-client-id-schemes.md) | [release]<br>fun [withClientIdSchemes](with-client-id-schemes.md)(clientIdSchemes: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../../-client-id-scheme/index.md)&gt;): [OpenId4VpConfig.Builder](index.md)<br>fun [withClientIdSchemes](with-client-id-schemes.md)(vararg clientIdSchemes: [ClientIdScheme](../../-client-id-scheme/index.md)): [OpenId4VpConfig.Builder](index.md)<br>Sets the supported client identifier schemes. |
| [withEncryptionAlgorithms](with-encryption-algorithms.md) | [release]<br>fun [withEncryptionAlgorithms](with-encryption-algorithms.md)(encryptionAlgorithms: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../../-encryption-algorithm/index.md)&gt;): [OpenId4VpConfig.Builder](index.md)<br>fun [withEncryptionAlgorithms](with-encryption-algorithms.md)(vararg encryptionAlgorithms: [EncryptionAlgorithm](../../-encryption-algorithm/index.md)): [OpenId4VpConfig.Builder](index.md)<br>Sets the list of supported encryption algorithms. |
| [withEncryptionMethods](with-encryption-methods.md) | [release]<br>fun [withEncryptionMethods](with-encryption-methods.md)(encryptionMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../../-encryption-method/index.md)&gt;): [OpenId4VpConfig.Builder](index.md)<br>fun [withEncryptionMethods](with-encryption-methods.md)(vararg encryptionMethods: [EncryptionMethod](../../-encryption-method/index.md)): [OpenId4VpConfig.Builder](index.md)<br>Sets the list of supported encryption methods. |
| [withFormats](with-formats.md) | [release]<br>fun [withFormats](with-formats.md)(formats: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Format](../../-format/index.md)&gt;): [OpenId4VpConfig.Builder](index.md)<br>fun [withFormats](with-formats.md)(vararg formats: [Format](../../-format/index.md)): [OpenId4VpConfig.Builder](index.md)<br>Sets the supported credential formats for the OpenID4VP. |
| [withSchemes](with-schemes.md) | [release]<br>fun [withSchemes](with-schemes.md)(schemes: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)&gt;): [OpenId4VpConfig.Builder](index.md)<br>Sets a list of schemes for OpenID4VP. By default, the scheme &quot;mdoc-openid4vp&quot; is supported.<br>[release]<br>fun [withSchemes](with-schemes.md)(vararg schemes: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)): [OpenId4VpConfig.Builder](index.md)<br>Sets a list of schemes for OpenID4VP. |