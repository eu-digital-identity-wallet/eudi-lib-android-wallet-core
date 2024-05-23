//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[Builder](index.md)

# Builder

class [Builder](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))

Builder to create an instance of [OpenId4VciManager](../index.md)

#### Parameters

androidJvm

| | |
|---|---|
| context | the context |

## Constructors

| | |
|---|---|
| [Builder](-builder.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html)) |

## Functions

| Name | Summary |
|---|---|
| [build](build.md) | [androidJvm]<br>fun [build](build.md)(): [OpenId4VciManager](../index.md)<br>Build the [OpenId4VciManager](../index.md) |
| [config](config.md) | [androidJvm]<br>fun [config](config.md)(config: [OpenId4VciManager.Config](../-config/index.md)): [OpenId4VciManager.Builder](index.md)<br>Set the [Config](../-config/index.md) to use |
| [documentManager](document-manager.md) | [androidJvm]<br>fun [documentManager](document-manager.md)(documentManager: DocumentManager): [OpenId4VciManager.Builder](index.md)<br>Set the DocumentManager to use |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [androidJvm]<br>var [config](config.md): [OpenId4VciManager.Config](../-config/index.md)?<br>the [Config](../-config/index.md) to use |
| [documentManager](document-manager.md) | [androidJvm]<br>var [documentManager](document-manager.md): DocumentManager?<br>the DocumentManager to use |
