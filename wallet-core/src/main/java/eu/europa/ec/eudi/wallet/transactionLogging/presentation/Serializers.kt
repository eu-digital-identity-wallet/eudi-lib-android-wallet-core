/*
 * Copyright (c) 2025 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Custom serializers for transaction logging presentation layer components.
 *
 * This file contains Kotlinx Serialization custom serializers for OpenID4VP-related data structures
 * used in transaction logging. The serializers handle complex nested structures and provide
 * JSON serialization/deserialization capabilities for:
 * - VP Token consensus data
 * - Verifiable presentations collections
 *
 * - Individual verifiable presentation objects
 */
package eu.europa.ec.eudi.wallet.transactionLogging.presentation

import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VerifiablePresentations
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule


val module = SerializersModule {
    contextual(Consensus.PositiveConsensus::class, VPTokenConsensusSerializer)
}

val VPTokenConsensusJson = Json {
    ignoreUnknownKeys = true
    serializersModule = module
}

/**
 * Custom serializer for [Consensus.PositiveConsensus] objects.
 *
 * This serializer handles the serialization and deserialization of VP Token consensus data,
 * which contains verifiable presentations that have been agreed upon during the consensus process.
 * The serializer delegates the actual presentations serialization to [VerifiablePresentationsSerializer].
 *
 * @see Consensus.PositiveConsensus
 * @see VerifiablePresentationsSerializer
 */
object VPTokenConsensusSerializer : KSerializer<Consensus.PositiveConsensus> {

    /**
     * Serial descriptor for the VPTokenConsensus structure.
     * Defines a single element "verifiablePresentations" of type String.
     */
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VPTokenConsensus") {
        element<String>("verifiablePresentations")
    }

    /**
     * Serializes a [Consensus.PositiveConsensus] object to the encoder.
     *
     * @param encoder The encoder to write the serialized data to
     * @param value The PositiveConsensus object to serialize
     */
    override fun serialize(encoder: Encoder, value: Consensus.PositiveConsensus) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(
                descriptor,
                0,
                VerifiablePresentationsSerializer,
                value.verifiablePresentations
            )
        }
    }

    /**
     * Deserializes a [Consensus.PositiveConsensus] object from the decoder.
     *
     * @param decoder The decoder to read the serialized data from
     * @return The deserialized PositiveConsensus object
     * @throws SerializationException if the required verifiablePresentations field is missing
     */
    override fun deserialize(decoder: Decoder): Consensus.PositiveConsensus {
        return decoder.decodeStructure(descriptor) {
            var verifiablePresentations: VerifiablePresentations? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> verifiablePresentations = decodeSerializableElement(
                        descriptor, 0,
                        VerifiablePresentationsSerializer
                    )

                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            Consensus.PositiveConsensus(
                verifiablePresentations = verifiablePresentations
                    ?: throw SerializationException("Missing verifiablePresentations")
            )
        }
    }
}

/**
 * Custom serializer for [VerifiablePresentations] collections.
 *
 * This serializer handles the complex structure of verifiable presentations, which are organized
 * as a map of query IDs to lists of presentation objects. The serializer converts the internal
 * map structure to a JSON-serializable format and vice versa.
 *
 * **Important**: This serializer only works with JSON encoding/decoding and will throw a
 * [SerializationException] if used with other formats.
 *
 * @see VerifiablePresentations
 * @see QueryId
 * @see VerifiablePresentation
 */
object VerifiablePresentationsSerializer : KSerializer<VerifiablePresentations> {

    /**
     * Serial descriptor for the VerifiablePresentations structure.
     * Defines a presentations map containing query ID keys and presentation lists.
     */
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("VerifiablePresentations") {
            element<Map<String, List<JsonElement>>>("presentations")
        }

    /**
     * Serializes a [VerifiablePresentations] object to JSON format.
     *
     * The method converts the internal map of [QueryId] to [VerifiablePresentation] lists
     * into a JSON object where query IDs become string keys and presentations become JSON arrays.
     *
     * @param encoder The JSON encoder to write the serialized data to
     * @param value The VerifiablePresentations object to serialize
     * @throws SerializationException if the encoder is not a JsonEncoder
     */
    override fun serialize(encoder: Encoder, value: VerifiablePresentations) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with JSON")

        // Convert the internal map to a serializable format
        val serializedMap = mutableMapOf<String, JsonArray>()
        value.value.forEach { (queryId, presentations) ->
            val serializedPresentations = presentations.map { presentation ->
                Json.encodeToJsonElement(
                    VerifiablePresentationSerializer,
                    presentation
                )
            }
            serializedMap[queryId.value] = JsonArray(serializedPresentations)
        }

        jsonEncoder.encodeJsonElement(Json.encodeToJsonElement(serializedMap))
    }

    /**
     * Deserializes a [VerifiablePresentations] object from JSON format.
     *
     * The method reconstructs the internal map structure from a JSON object, converting
     * string keys back to [QueryId] objects and JSON arrays back to presentation lists.
     *
     * @param decoder The JSON decoder to read the serialized data from
     * @return The deserialized VerifiablePresentations object
     * @throws SerializationException if the decoder is not a JsonDecoder
     */
    override fun deserialize(decoder: Decoder): VerifiablePresentations {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can only be used with JSON")
        val jsonElement = jsonDecoder.decodeJsonElement()
        val jsonObject = jsonElement.jsonObject

        val presentations = mutableMapOf<QueryId, List<VerifiablePresentation>>()
        jsonObject.forEach { (key, value) ->
            val queryId = QueryId(key)
            val presentationsList = value.jsonArray.map { presentationElement ->
                Json.decodeFromJsonElement(VerifiablePresentationSerializer, presentationElement)
            }
            presentations[queryId] = presentationsList
        }

        return VerifiablePresentations(presentations)
    }
}

/**
 * Custom serializer for [VerifiablePresentation] objects.
 *
 * This serializer handles the polymorphic nature of verifiable presentations, which can be
 * either generic string presentations or JSON object presentations. The serializer uses a
 * type field to distinguish between the different presentation types during serialization
 * and deserialization.
 *
 * Supported presentation types:
 * - [VerifiablePresentation.Generic]: Simple string-based presentations
 * - [VerifiablePresentation.JsonObj]: JSON object-based presentations
 *
 * @see VerifiablePresentation
 * @see VerifiablePresentation.Generic
 * @see VerifiablePresentation.JsonObj
 */
object VerifiablePresentationSerializer : KSerializer<VerifiablePresentation> {

    /**
     * Serial descriptor for the VerifiablePresentation structure.
     * Defines two elements: "type" for the presentation type and "value" for the content.
     */
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("VerifiablePresentation") {
            element<String>("type")
            element<String>("value")
        }

    /**
     * Serializes a [VerifiablePresentation] object based on its concrete type.
     *
     * The method determines the presentation type and serializes both the type identifier
     * and the presentation content accordingly:
     * - Generic presentations are serialized with their string value
     * - JsonObj presentations are serialized with their JSON object converted to string
     *
     * @param encoder The encoder to write the serialized data to
     * @param value The VerifiablePresentation object to serialize
     */
    override fun serialize(
        encoder: Encoder,
        value: VerifiablePresentation,
    ) {
        encoder.encodeStructure(descriptor) {
            when (value) {
                is VerifiablePresentation.Generic -> {
                    encodeStringElement(descriptor, 0, "Generic")
                    encodeStringElement(descriptor, 1, value.value)
                }

                is VerifiablePresentation.JsonObj -> {
                    encodeStringElement(descriptor, 0, "JsonObj")
                    encodeStringElement(descriptor, 1, value.value.toString())
                }
            }
        }
    }

    /**
     * Deserializes a [VerifiablePresentation] object from the encoded data.
     *
     * The method reads the type identifier and value, then reconstructs the appropriate
     * concrete presentation type based on the type field. For JsonObj types, the string
     * value is parsed back into a JSON object.
     *
     * @param decoder The decoder to read the serialized data from
     * @return The deserialized VerifiablePresentation object
     * @throws SerializationException if required fields are missing or if an unknown type is encountered
     */
    override fun deserialize(decoder: Decoder): VerifiablePresentation {
        return decoder.decodeStructure(descriptor) {
            var type: String? = null
            var value: String? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> value = decodeStringElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            val typeValue = type ?: throw SerializationException("Missing type")
            val stringValue = value ?: throw SerializationException("Missing value")

            when (typeValue) {
                "Generic" -> VerifiablePresentation.Generic(stringValue)
                "JsonObj" -> VerifiablePresentation.JsonObj(Json.parseToJsonElement(stringValue).jsonObject)
                else -> throw SerializationException("Unknown VerifiablePresentation type: $typeValue")
            }

        }
    }
}