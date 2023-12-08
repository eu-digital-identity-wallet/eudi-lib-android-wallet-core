//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[startEngagementToApp](start-engagement-to-app.md)

# startEngagementToApp

[androidJvm]\
fun [startEngagementToApp](start-engagement-to-app.md)(
intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))

Starts the transfer process by engaging with the reader via appLink

#### Parameters

androidJvm

|        |
|--------|
| intent |

#### See also

|                                      |
|--------------------------------------|
| TransferManager.startEngagementToApp |

#### Throws

|                                                                                                                  |                                                                                     |
|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
