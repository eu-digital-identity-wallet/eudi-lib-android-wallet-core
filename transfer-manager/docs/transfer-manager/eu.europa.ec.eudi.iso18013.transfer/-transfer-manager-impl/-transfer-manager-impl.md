//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferManagerImpl](index.md)/[TransferManagerImpl](-transfer-manager-impl.md)

# TransferManagerImpl

[release]\

@[JvmOverloads](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)

constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), requestProcessor: [RequestProcessor](../../eu.europa.ec.eudi.iso18013.transfer.response/-request-processor/index.md), retrievalMethods: [List](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[DeviceRetrievalMethod](../../eu.europa.ec.eudi.iso18013.transfer.engagement/-device-retrieval-method/index.md)&gt;? = null)

Create a Transfer Manager

#### Parameters

release

| | |
|---|---|
| context | application context |
| requestProcessor | request processor for processing the device request and generating the response |
| retrievalMethods | list of device retrieval methods to be used for the transfer |