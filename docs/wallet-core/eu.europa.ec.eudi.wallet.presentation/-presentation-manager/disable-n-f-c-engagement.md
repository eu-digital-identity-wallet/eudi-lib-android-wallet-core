//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManager](index.md)/[disableNFCEngagement](disable-n-f-c-engagement.md)

# disableNFCEngagement

[androidJvm]\
abstract fun [disableNFCEngagement](disable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManager](index.md)

Disable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onPause](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html#onpause) method.

#### Return

this [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| activity | the activity |
