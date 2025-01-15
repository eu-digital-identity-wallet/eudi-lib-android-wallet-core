//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpConfig](index.md)

# OpenId4VpConfig

[androidJvm]\
class [OpenId4VpConfig](index.md)

Configuration for the OpenId4Vp transfer.

Use [Builder](-builder/index.md) to create an instance.

Example usage:

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
             ClientIdScheme.X509SanDns
         )
    )
    .withScheme(
        listOf(
            "eudi-openid4vp",
            "mdoc-openid4vp"
        )
    )
   .withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
   .withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))
   .withFormats(Format.MsoMdoc, Format.SdJwtVc.ES256)
   .build()
```

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)<br>Builder for OpenId4VciConfig. |

## Properties

| Name | Summary |
|---|---|
| [clientIdSchemes](client-id-schemes.md) | [androidJvm]<br>val [clientIdSchemes](client-id-schemes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../-client-id-scheme/index.md)&gt;<br>list of [ClientIdScheme](../-client-id-scheme/index.md) that defines the supported Client Identifier schemes |
| [encryptionAlgorithms](encryption-algorithms.md) | [androidJvm]<br>val [encryptionAlgorithms](encryption-algorithms.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../-encryption-algorithm/index.md)&gt;<br>list of [EncryptionAlgorithm](../-encryption-algorithm/index.md) that defines the supported encryption algorithms |
| [encryptionMethods](encryption-methods.md) | [androidJvm]<br>val [encryptionMethods](encryption-methods.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../-encryption-method/index.md)&gt;<br>list of [EncryptionMethod](../-encryption-method/index.md) that defines the supported encryption methods |
| [formats](formats.md) | [androidJvm]<br>val [formats](formats.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Format](../-format/index.md)&gt;<br>the supported credential formats |
| [schemes](schemes.md) | [androidJvm]<br>val [schemes](schemes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>optionally you can set one or more schemes. By default, the scheme &quot;mdoc-openid4vp&quot; is used |
