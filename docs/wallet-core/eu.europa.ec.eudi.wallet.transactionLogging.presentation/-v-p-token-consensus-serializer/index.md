//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VPTokenConsensusSerializer](index.md)

# VPTokenConsensusSerializer

object [VPTokenConsensusSerializer](index.md) : KSerializer&lt;Consensus.PositiveConsensus&gt; 

Custom serializer for Consensus.PositiveConsensus objects.

This serializer handles the serialization and deserialization of VP Token consensus data, which contains verifiable presentations that have been agreed upon during the consensus process. The serializer delegates the actual presentations serialization to [VerifiablePresentationsSerializer](../-verifiable-presentations-serializer/index.md).

#### See also

| |
|---|
| Consensus.PositiveConsensus |
| [VerifiablePresentationsSerializer](../-verifiable-presentations-serializer/index.md) |

## Properties

| Name | Summary |
|---|---|
| [descriptor](descriptor.md) | [androidJvm]<br>open override val [descriptor](descriptor.md): SerialDescriptor<br>Serial descriptor for the VPTokenConsensus structure. Defines a single element &quot;verifiablePresentations&quot; of type String. |

## Functions

| Name | Summary |
|---|---|
| [deserialize](deserialize.md) | [androidJvm]<br>open override fun [deserialize](deserialize.md)(decoder: Decoder): Consensus.PositiveConsensus<br>Deserializes a Consensus.PositiveConsensus object from the decoder. |
| [serialize](serialize.md) | [androidJvm]<br>open override fun [serialize](serialize.md)(encoder: Encoder, value: Consensus.PositiveConsensus)<br>Serializes a Consensus.PositiveConsensus object to the encoder. |
