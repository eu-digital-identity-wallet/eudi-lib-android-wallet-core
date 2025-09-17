//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VerifiablePresentationSerializer](index.md)/[deserialize](deserialize.md)

# deserialize

[androidJvm]\
open override fun [deserialize](deserialize.md)(decoder: Decoder): VerifiablePresentation

Deserializes a VerifiablePresentation object from the encoded data.

The method reads the type identifier and value, then reconstructs the appropriate concrete presentation type based on the type field. For JsonObj types, the string value is parsed back into a JSON object.

#### Return

The deserialized VerifiablePresentation object

#### Parameters

androidJvm

| | |
|---|---|
| decoder | The decoder to read the serialized data from |

#### Throws

| | |
|---|---|
| SerializationException | if required fields are missing or if an unknown type is encountered |
