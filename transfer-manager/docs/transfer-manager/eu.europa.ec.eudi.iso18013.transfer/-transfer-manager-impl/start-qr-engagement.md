//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferManagerImpl](index.md)/[startQrEngagement](start-qr-engagement.md)

# startQrEngagement

[release]\
open override fun [startQrEngagement](start-qr-engagement.md)()

Starts the QR Engagement and generates the QR code Once the QR code is ready, the event [TransferEvent.QrEngagementReady](../-transfer-event/-qr-engagement-ready/index.md) will be triggered If the transfer has already started, an error event will be triggered with an [IllegalStateException](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-illegal-state-exception/index.html) containing the message &quot;Transfer has already started.&quot;

#### See also

| |
|---|
| [TransferEvent.QrEngagementReady](../-transfer-event/-qr-engagement-ready/index.md) |
| [TransferEvent.Error](../-transfer-event/-error/index.md) |