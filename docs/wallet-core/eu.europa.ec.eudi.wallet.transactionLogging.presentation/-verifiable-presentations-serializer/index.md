//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VerifiablePresentationsSerializer](index.md)

# VerifiablePresentationsSerializer

object [VerifiablePresentationsSerializer](index.md) : KSerializer&lt;VerifiablePresentations&gt; 

Custom serializer for VerifiablePresentations collections.

This serializer handles the complex structure of verifiable presentations, which are organized as a map of query IDs to lists of presentation objects. The serializer converts the internal map structure to a JSON-serializable format and vice versa.

**Important**: This serializer only works with JSON encoding/decoding and will throw a SerializationException if used with other formats.

#### See also

| |
|---|
| VerifiablePresentations |
| QueryId |
| VerifiablePresentation |

## Properties

| Name | Summary |
|---|---|
| [descriptor](descriptor.md) | [androidJvm]<br>open override val [descriptor](descriptor.md): SerialDescriptor<br>Serial descriptor for the VerifiablePresentations structure. Defines a presentations map containing query ID keys and presentation lists. |

## Functions

| Name | Summary |
|---|---|
| [deserialize](deserialize.md) | [androidJvm]<br>open override fun [deserialize](deserialize.md)(decoder: Decoder): VerifiablePresentations<br>Deserializes a VerifiablePresentations object from JSON format. |
| [serialize](serialize.md) | [androidJvm]<br>open override fun [serialize](serialize.md)(encoder: Encoder, value: VerifiablePresentations)<br>Serializes a VerifiablePresentations object to JSON format. |
