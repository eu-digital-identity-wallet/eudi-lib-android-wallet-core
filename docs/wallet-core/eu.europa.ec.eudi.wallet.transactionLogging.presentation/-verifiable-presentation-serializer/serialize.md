//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VerifiablePresentationSerializer](index.md)/[serialize](serialize.md)

# serialize

[androidJvm]\
open override fun [serialize](serialize.md)(encoder: Encoder, value: VerifiablePresentation)

Serializes a VerifiablePresentation object based on its concrete type.

The method determines the presentation type and serializes both the type identifier and the presentation content accordingly:

- 
   Generic presentations are serialized with their string value
- 
   JsonObj presentations are serialized with their JSON object converted to string

#### Parameters

androidJvm

| | |
|---|---|
| encoder | The encoder to write the serialized data to |
| value | The VerifiablePresentation object to serialize |
