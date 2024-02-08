//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](../../index.md)/[OpenId4VpConfig](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)

Builder for [OpenId4VciConfig](../../../eu.europa.ec.eudi.wallet.document.issue.openid4vci/-open-id4-vci-config/index.md).

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [OpenId4VpConfig](../index.md) |
| [withClientIdSchemes](with-client-id-schemes.md) | [androidJvm]<br>fun [withClientIdSchemes](with-client-id-schemes.md)(clientIdSchemes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../../-client-id-scheme/index.md)&gt;): [OpenId4VpConfig.Builder](index.md)<br>Sets the issuer url. |
| [withEncryptionAlgorithms](with-encryption-algorithms.md) | [androidJvm]<br>fun [withEncryptionAlgorithms](with-encryption-algorithms.md)(encryptionAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../../-encryption-algorithm/index.md)&gt;): [OpenId4VpConfig.Builder](index.md)<br>Sets the issuer url. |
| [withEncryptionMethods](with-encryption-methods.md) | [androidJvm]<br>fun [withEncryptionMethods](with-encryption-methods.md)(encryptionMethods: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../../-encryption-method/index.md)&gt;): [OpenId4VpConfig.Builder](index.md)<br>Sets the issuer url. |
| [withScheme](with-scheme.md) | [androidJvm]<br>fun [withScheme](with-scheme.md)(scheme: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [OpenId4VpConfig.Builder](index.md)<br>Sets the scheme for openId4Vp. By default, the scheme &quot;mdoc-openid4vp&quot; is supported |

## Properties

| Name | Summary |
|---|---|
| [clientIdSchemes](client-id-schemes.md) | [androidJvm]<br>lateinit var [clientIdSchemes](client-id-schemes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../../-client-id-scheme/index.md)&gt;<br>list of [ClientIdScheme](../../-client-id-scheme/index.md) that defines the supported Client Identifier schemes |
| [encryptionAlgorithms](encryption-algorithms.md) | [androidJvm]<br>lateinit var [encryptionAlgorithms](encryption-algorithms.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../../-encryption-algorithm/index.md)&gt;<br>list of [EncryptionAlgorithm](../../-encryption-algorithm/index.md) that defines the supported encryption algorithms |
| [encryptionMethods](encryption-methods.md) | [androidJvm]<br>lateinit var [encryptionMethods](encryption-methods.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../../-encryption-method/index.md)&gt;<br>list of [EncryptionMethod](../../-encryption-method/index.md) that defines the supported encryption methods |
| [scheme](scheme.md) | [androidJvm]<br>var [scheme](scheme.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>for OpenId4Vp. Optionally, you can change the scheme. By default, &quot;mdoc-openid4vp&quot; is used. |
