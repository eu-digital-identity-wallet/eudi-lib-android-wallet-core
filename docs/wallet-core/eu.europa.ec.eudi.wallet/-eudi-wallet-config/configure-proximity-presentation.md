//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureProximityPresentation](configure-proximity-presentation.md)

# configureProximityPresentation

[androidJvm]\

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)

fun [configureProximityPresentation](configure-proximity-presentation.md)(enableBlePeripheralMode: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = true, enableBleCentralMode: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false, clearBleCache: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = true, nfcEngagementServiceClass: [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;out NfcEngagementService&gt;? = null): &lt;Error class: unknown class&gt;

Configure the proximity presentation. This allows to configure the BLE peripheral mode, the BLE central mode and whether to clear the BLE cache. Also, it allows to set the NFC engagement service class an implementation of NfcEngagementService, which is used to handle the NFC engagement.

The default values are:

- 
   enableBlePeripheralMode: true
- 
   enableBleCentralMode: false
- 
   clearBleCache: true
- 
   nfcEngagementServiceClass: null

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| enableBlePeripheralMode | whether to enable BLE peripheral mode |
| enableBleCentralMode | whether to enable BLE central mode |
| clearBleCache | whether to clear the BLE cache |
| nfcEngagementServiceClass | the NFC engagement service class |
