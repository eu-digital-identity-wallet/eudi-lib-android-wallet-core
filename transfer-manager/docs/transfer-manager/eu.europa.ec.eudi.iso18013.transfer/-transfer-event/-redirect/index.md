//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../../index.md)/[TransferEvent](../index.md)/[Redirect](index.md)

# Redirect

[release]\
data class [Redirect](index.md)(val redirectUri: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)) : [TransferEvent](../index.md)

Redirect event. This event is triggered when a redirect is needed. This event is to be used for implementation for the OpenId4VP protocol.

## Constructors

| | |
|---|---|
| [Redirect](-redirect.md) | [release]<br>constructor(redirectUri: [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)) |

## Properties

| Name | Summary |
|---|---|
| [redirectUri](redirect-uri.md) | [release]<br>val [redirectUri](redirect-uri.md): [URI](https://developer.android.com/reference/kotlin/java/net/URI.html)<br>the redirect URI |