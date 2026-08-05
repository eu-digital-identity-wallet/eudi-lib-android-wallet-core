//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../../index.md)/[TransferEvent](../index.md)/[IntentToSend](index.md)

# IntentToSend

[release]\
data class [IntentToSend](index.md)(val intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html)) : [TransferEvent](../index.md)

Intent to send event. This event is triggered when an intent is to be sent. This event is to be used for implementation of Digital Credentials API.

## Constructors

| | |
|---|---|
| [IntentToSend](-intent-to-send.md) | [release]<br>constructor(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html)) |

## Properties

| Name | Summary |
|---|---|
| [intent](intent.md) | [release]<br>val [intent](intent.md): [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html)<br>the intent to be sent |