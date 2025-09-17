//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VerifiablePresentationsSerializer](index.md)/[serialize](serialize.md)

# serialize

[androidJvm]\
open override fun [serialize](serialize.md)(encoder: Encoder, value: VerifiablePresentations)

Serializes a VerifiablePresentations object to JSON format.

The method converts the internal map of QueryId to VerifiablePresentation lists into a JSON object where query IDs become string keys and presentations become JSON arrays.

#### Parameters

androidJvm

| | |
|---|---|
| encoder | The JSON encoder to write the serialized data to |
| value | The VerifiablePresentations object to serialize |

#### Throws

| | |
|---|---|
| SerializationException | if the encoder is not a JsonEncoder |
