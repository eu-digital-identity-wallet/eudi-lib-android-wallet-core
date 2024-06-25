//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](../../index.md)/[OpenId4VpCBORResponseGeneratorImpl](../index.md)/[Builder](index.md)

# Builder

[androidJvm]\
class [Builder](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [OpenId4VpCBORResponseGeneratorImpl](../index.md) |
| [readerTrustStore](reader-trust-store.md) | [androidJvm]<br>fun [readerTrustStore](reader-trust-store.md)(readerTrustStore: ReaderTrustStore): [OpenId4VpCBORResponseGeneratorImpl.Builder](index.md)<br>Reader trust store that will be used to validate the certificate chain of the mdoc verifier |

## Properties

| Name                                       | Summary                                                                                                        |
|--------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| [documentsResolver](documents-resolver.md) | [androidJvm]<br>var [documentsResolver](documents-resolver.md): DocumentsResolver?                             |
| [logger](logger.md)                        | [androidJvm]<br>var [logger](logger.md): [Logger](../../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? |
| [readerTrustStore](reader-trust-store.md)  | [androidJvm]<br>var [readerTrustStore](reader-trust-store.md): ReaderTrustStore?                               |
