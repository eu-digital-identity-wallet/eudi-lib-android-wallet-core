//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManagerImpl](index.md)/[disableNFCEngagement](disable-n-f-c-engagement.md)

# disableNFCEngagement

[release]\
open override fun [disableNFCEngagement](disable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html)): [PresentationManagerImpl](index.md)

Disable the NFC device engagement for the wallet. This method should be called in the activity's [ComponentActivity.onPause](https://developer.android.com/reference/kotlin/android/app/Activity.html#onpause) method.

#### Return

this [EudiWallet](../../eu.europa.ec.eudi.wallet/-eudi-wallet/index.md) instance

#### Parameters

release

| | |
|---|---|
| activity | the activity |