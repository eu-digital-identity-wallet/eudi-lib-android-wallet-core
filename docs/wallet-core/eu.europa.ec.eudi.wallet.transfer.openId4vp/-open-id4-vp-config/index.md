//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpConfig](index.md)

# OpenId4VpConfig

class [OpenId4VpConfig](index.md)

Configuration for OpenID4VP (OpenID for Verifiable Presentations) transfer operations.

This class provides comprehensive configuration options for OpenID4VP protocol implementation, including support for various client identification schemes, encryption algorithms, credential formats, and protocol schemes. It follows the OpenID4VP specification for secure verifiable credential presentations.

## Example Usage:

```kotlin
val config = OpenId4VpConfig.Builder()
   .withClientIdSchemes(
         listOf(
             ClientIdScheme.Preregistered(
                 listOf(
                     PreregisteredVerifier(
                     "VerifierClientId",
                     "VerifierLegalName",
                     "http://example.com")
                 )),
             ClientIdScheme.X509SanDns,
             ClientIdScheme.X509Hash,
             ClientIdScheme.RedirectUri
         )
    )
    .withSchemes(
        listOf(
            "eudi-openid4vp",
            "mdoc-openid4vp"
        )
    )
   .withEncryptionAlgorithms(EncryptionAlgorithm.SUPPORTED_ENCRYPTION_ALGORITHMS) //optional; if not set, all supported algorithms will be used
   .withEncryptionMethods(EncryptionMethod.SUPPORTED_ENCRYPTION_METHODS) //optional; if not set, all supported methods will be used
   .withFormats(Format.MsoMdoc.ES256, Format.SdJwtVc.ES256)
   .build()
```

#### Since

1.0.0

#### See also

| |
|---|
| [ClientIdScheme](../-client-id-scheme/index.md) |
| [EncryptionAlgorithm](../-encryption-algorithm/index.md) |
| [EncryptionMethod](../-encryption-method/index.md) |
| [Format](../-format/index.md) |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)<br>Builder for constructing [OpenId4VpConfig](index.md) instances with validation and sensible defaults. |

## Properties

| Name | Summary |
|---|---|
| [clientIdSchemes](client-id-schemes.md) | [androidJvm]<br>val [clientIdSchemes](client-id-schemes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../-client-id-scheme/index.md)&gt;<br>List of supported client identifier schemes that define how verifiers authenticate themselves |
| [encryptionAlgorithms](encryption-algorithms.md) | [androidJvm]<br>val [encryptionAlgorithms](encryption-algorithms.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../-encryption-algorithm/index.md)&gt;<br>List of supported encryption algorithms for securing communication channels |
| [encryptionMethods](encryption-methods.md) | [androidJvm]<br>val [encryptionMethods](encryption-methods.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../-encryption-method/index.md)&gt;<br>List of supported encryption methods for content encryption |
| [formats](formats.md) | [androidJvm]<br>val [formats](formats.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Format](../-format/index.md)&gt;<br>Supported credential formats (mDL/mDoc, SD-JWT VC, etc.) |
| [schemes](schemes.md) | [androidJvm]<br>val [schemes](schemes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>URI schemes supported for OpenID4VP requests (default: &quot;mdoc-openid4vp&quot;) |
