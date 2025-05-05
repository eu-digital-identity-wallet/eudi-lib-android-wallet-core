//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../../index.md)/[OpenId4VpConfig](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)

Builder for OpenId4VciConfig.

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [clientIdSchemes](client-id-schemes.md) | [androidJvm]<br>lateinit var [clientIdSchemes](client-id-schemes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../../-client-id-scheme/index.md)&gt;<br>list of [ClientIdScheme](../../-client-id-scheme/index.md) that defines the supported Client Identifier schemes |
| [encryptionAlgorithms](encryption-algorithms.md) | [androidJvm]<br>lateinit var [encryptionAlgorithms](encryption-algorithms.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../../-encryption-algorithm/index.md)&gt;<br>list of [EncryptionAlgorithm](../../-encryption-algorithm/index.md) that defines the supported encryption algorithms |
| [encryptionMethods](encryption-methods.md) | [androidJvm]<br>lateinit var [encryptionMethods](encryption-methods.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../../-encryption-method/index.md)&gt;<br>list of [EncryptionMethod](../../-encryption-method/index.md) that defines the supported encryption methods |
| [formats](formats.md) | [androidJvm]<br>lateinit var [formats](formats.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Format](../../-format/index.md)&gt;<br>the supported credential formats |
| [schemes](schemes.md) | [androidJvm]<br>var [schemes](schemes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>for OpenId4Vp. Optionally, you can set one or more schemes. By default, &quot;mdoc-openid4vp&quot; is used. |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [OpenId4VpConfig](../index.md)<br>Builds the [OpenId4VpConfig](../index.md). |
| [withClientIdSchemes](with-client-id-schemes.md) | [androidJvm]<br>fun [withClientIdSchemes](with-client-id-schemes.md)(vararg clientIdSchemes: [ClientIdScheme](../../-client-id-scheme/index.md)): &lt;Error class: unknown class&gt;<br>fun [withClientIdSchemes](with-client-id-schemes.md)(clientIdSchemes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[ClientIdScheme](../../-client-id-scheme/index.md)&gt;): &lt;Error class: unknown class&gt;<br>Sets the supported client identifier schemes. |
| [withEncryptionAlgorithms](with-encryption-algorithms.md) | [androidJvm]<br>fun [withEncryptionAlgorithms](with-encryption-algorithms.md)(vararg encryptionAlgorithms: [EncryptionAlgorithm](../../-encryption-algorithm/index.md)): &lt;Error class: unknown class&gt;<br>fun [withEncryptionAlgorithms](with-encryption-algorithms.md)(encryptionAlgorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionAlgorithm](../../-encryption-algorithm/index.md)&gt;): &lt;Error class: unknown class&gt;<br>Sets the list of supported encryption algorithms. |
| [withEncryptionMethods](with-encryption-methods.md) | [androidJvm]<br>fun [withEncryptionMethods](with-encryption-methods.md)(vararg encryptionMethods: [EncryptionMethod](../../-encryption-method/index.md)): &lt;Error class: unknown class&gt;<br>fun [withEncryptionMethods](with-encryption-methods.md)(encryptionMethods: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[EncryptionMethod](../../-encryption-method/index.md)&gt;): &lt;Error class: unknown class&gt;<br>Sets the list of supported encryption methods. |
| [withFormats](with-formats.md) | [androidJvm]<br>fun [withFormats](with-formats.md)(vararg formats: [Format](../../-format/index.md)): &lt;Error class: unknown class&gt;<br>fun [withFormats](with-formats.md)(formats: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Format](../../-format/index.md)&gt;): &lt;Error class: unknown class&gt;<br>Sets the supported credential formats for the OpenID4VP. |
| [withSchemes](with-schemes.md) | [androidJvm]<br>fun [withSchemes](with-schemes.md)(vararg schemes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): &lt;Error class: unknown class&gt;<br>Sets a list of schemes for OpenID4VP.<br>[androidJvm]<br>fun [withSchemes](with-schemes.md)(schemes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;): &lt;Error class: unknown class&gt;<br>Sets a list of schemes for OpenID4VP. By default, the scheme &quot;mdoc-openid4vp&quot; is supported. |
