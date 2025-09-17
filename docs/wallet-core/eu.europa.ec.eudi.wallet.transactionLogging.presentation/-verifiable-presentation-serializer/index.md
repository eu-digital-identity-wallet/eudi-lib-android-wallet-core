//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transactionLogging.presentation](../index.md)/[VerifiablePresentationSerializer](index.md)

# VerifiablePresentationSerializer

object [VerifiablePresentationSerializer](index.md) : KSerializer&lt;VerifiablePresentation&gt; 

Custom serializer for VerifiablePresentation objects.

This serializer handles the polymorphic nature of verifiable presentations, which can be either generic string presentations or JSON object presentations. The serializer uses a type field to distinguish between the different presentation types during serialization and deserialization.

Supported presentation types:

- 
   VerifiablePresentation.Generic: Simple string-based presentations
- 
   VerifiablePresentation.JsonObj: JSON object-based presentations

#### See also

| |
|---|
| VerifiablePresentation |
| VerifiablePresentation.Generic |
| VerifiablePresentation.JsonObj |

## Properties

| Name | Summary |
|---|---|
| [descriptor](descriptor.md) | [androidJvm]<br>open override val [descriptor](descriptor.md): SerialDescriptor<br>Serial descriptor for the VerifiablePresentation structure. Defines two elements: &quot;type&quot; for the presentation type and &quot;value&quot; for the content. |

## Functions

| Name | Summary |
|---|---|
| [deserialize](deserialize.md) | [androidJvm]<br>open override fun [deserialize](deserialize.md)(decoder: Decoder): VerifiablePresentation<br>Deserializes a VerifiablePresentation object from the encoded data. |
| [serialize](serialize.md) | [androidJvm]<br>open override fun [serialize](serialize.md)(encoder: Encoder, value: VerifiablePresentation)<br>Serializes a VerifiablePresentation object based on its concrete type. |
