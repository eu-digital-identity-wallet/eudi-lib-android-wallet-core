//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VerifiablePresentationsSerializer](index.md)/[deserialize](deserialize.md)

# deserialize

[androidJvm]\
open override fun [deserialize](deserialize.md)(decoder: Decoder): VerifiablePresentations

Deserializes a VerifiablePresentations object from JSON format.

The method reconstructs the internal map structure from a JSON object, converting string keys back to QueryId objects and JSON arrays back to presentation lists.

#### Return

The deserialized VerifiablePresentations object

#### Parameters

androidJvm

| | |
|---|---|
| decoder | The JSON decoder to read the serialized data from |

#### Throws

| | |
|---|---|
| SerializationException | if the decoder is not a JsonDecoder |
