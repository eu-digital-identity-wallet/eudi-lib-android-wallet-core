//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VPTokenConsensusSerializer](index.md)/[deserialize](deserialize.md)

# deserialize

[androidJvm]\
open override fun [deserialize](deserialize.md)(decoder: Decoder): Consensus.PositiveConsensus

Deserializes a Consensus.PositiveConsensus object from the decoder.

#### Return

The deserialized PositiveConsensus object

#### Parameters

androidJvm

| | |
|---|---|
| decoder | The decoder to read the serialized data from |

#### Throws

| | |
|---|---|
| SerializationException | if the required verifiablePresentations field is missing |
