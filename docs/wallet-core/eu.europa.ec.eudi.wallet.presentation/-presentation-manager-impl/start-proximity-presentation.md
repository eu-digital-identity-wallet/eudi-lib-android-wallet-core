//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManagerImpl](index.md)/[startProximityPresentation](start-proximity-presentation.md)

# startProximityPresentation

[androidJvm]\
open override fun [startProximityPresentation](start-proximity-presentation.md)()

Starts the proximity presentation. The QR code is available through the TransferEvent.QrEngagementReady event which is triggered almost immediately after calling this method. The wallet should display the QR code to the verifier in order to start the proximity presentation.
