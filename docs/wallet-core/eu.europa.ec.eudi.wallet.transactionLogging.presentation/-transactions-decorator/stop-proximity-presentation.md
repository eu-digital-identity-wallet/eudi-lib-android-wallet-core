//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[TransactionsDecorator](index.md)/[stopProximityPresentation](stop-proximity-presentation.md)

# stopProximityPresentation

[androidJvm]\
open override fun [stopProximityPresentation](stop-proximity-presentation.md)(flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Stops the proximity presentation. Method receives flags that can be used to control the session termination. The available flags are:

- 
   SEND_SESSION_TERMINATION_MESSAGE: sends the session termination message to the verifier
- 
   SessionTerminationFlag.USE_TRANSPORT_SPECIFIC_SESSION_TERMINATION: uses the transport specific session termination

#### Parameters

androidJvm

| | |
|---|---|
| flags | the flags |
