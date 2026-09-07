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
 * JSON serializers for the OpenID4VP VP token types used in transaction logging: the consensus, the
 * collection of verifiable presentations, and a single presentation.
 */
package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation

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
 * Serializer for [Consensus.PositiveConsensus]. Delegates the presentations to
 * [VerifiablePresentationsSerializer].
 */
object VPTokenConsensusSerializer : KSerializer<Consensus.PositiveConsensus> {

    /** One element: "verifiablePresentations". */
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VPTokenConsensus") {
        element<String>("verifiablePresentations")
    }

    /** Writes the consensus's verifiable presentations. */
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
     * Reads a [Consensus.PositiveConsensus].
     *
     * @throws SerializationException if verifiablePresentations is missing.
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
 * Serializer for [VerifiablePresentations], a map of query id to a list of presentations, to and from
 * a JSON object. JSON only; throws [SerializationException] with any other format.
 */
object VerifiablePresentationsSerializer : KSerializer<VerifiablePresentations> {

    /** One "presentations" map: query id to a list of presentations. */
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("VerifiablePresentations") {
            element<Map<String, List<JsonElement>>>("presentations")
        }

    /**
     * Writes the presentations as a JSON object: query id keys, presentation arrays.
     *
     * @throws SerializationException if the encoder is not a JsonEncoder.
     */
    override fun serialize(encoder: Encoder, value: VerifiablePresentations) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with JSON")

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
     * Reads the presentations back from a JSON object, turning keys into [QueryId]s.
     *
     * @throws SerializationException if the decoder is not a JsonDecoder.
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
 * Serializer for [VerifiablePresentation]. A "type" field tells the two kinds apart:
 * [VerifiablePresentation.Generic] (a string) and [VerifiablePresentation.JsonObj] (a JSON object).
 */
object VerifiablePresentationSerializer : KSerializer<VerifiablePresentation> {

    /** Two elements: "type" and "value". */
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("VerifiablePresentation") {
            element<String>("type")
            element<String>("value")
        }

    /** Writes the type tag and the value (the JSON object is written as a string). */
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
     * Reads the type and value back into the matching presentation type.
     *
     * @throws SerializationException if a field is missing or the type is unknown.
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