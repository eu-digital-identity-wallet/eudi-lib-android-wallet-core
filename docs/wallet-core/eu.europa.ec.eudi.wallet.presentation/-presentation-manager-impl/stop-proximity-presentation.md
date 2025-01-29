//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManagerImpl](index.md)/[stopProximityPresentation](stop-proximity-presentation.md)

# stopProximityPresentation

[androidJvm]\
open override fun [stopProximityPresentation](stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Stops the proximity presentation. Method receives flags that can be used to control the session termination. The available flags are:

- 
   [SEND_SESSION_TERMINATION_MESSAGE](../-session-termination-flag/-companion/-s-e-n-d_-s-e-s-s-i-o-n_-t-e-r-m-i-n-a-t-i-o-n_-m-e-s-s-a-g-e.md): sends the session termination message to the verifier
- 
   [SessionTerminationFlag.USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION](../-session-termination-flag/-companion/-u-s-e_-t-r-a-n-s-p-o-r-t_-s-p-e-c-i-f-i-c_-s-e-s-s-i-o-n_-t-e-r-m-i-n-a-t-i-o-n.md): uses the transport specific session termination

#### Parameters

androidJvm

| | |
|---|---|
| flags | the flags |
