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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import android.annotation.SuppressLint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Points to a claim (or set of claims) inside a credential (OpenID4VP §7).
 *
 * A non-empty list of [Segment]s. Each segment is one of:
 * - [Segment.Key] — a JSON object key, or an mdoc `[namespace, dataElement]`.
 * - [Segment.Index] — a non-negative array index, e.g. `["documents", 0, "type"]`.
 * - [Segment.Wildcard] — every element of the array here, e.g. `["nationalities", null]`.
 *
 * On the wire it's a JSON array of strings, non-negative integers, and `null` (OpenID4VP §7.3,
 * also TS10 §3.19.2). Examples:
 * ```
 * ["family_name"]              → ofKeys("family_name")
 * ["address", "locality"]      → ofKeys("address", "locality")
 * ["nationalities", null]      → ClaimPath(Key("nationalities"), Wildcard)
 * ```
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable(with = ClaimPathSerializer::class)
data class ClaimPath(val segments: List<Segment>) {

    init {
        require(segments.isNotEmpty()) { "ClaimPath must be non-empty (OpenID4VP §7)" }
    }

    /** Return a new path with [segment] added at the end. */
    fun append(segment: Segment): ClaimPath = ClaimPath(segments + segment)

    /** Append a string key segment. */
    fun appendKey(name: String): ClaimPath = append(Segment.Key(name))

    /** Append an array-index segment. The index must be non-negative. */
    fun appendIndex(value: Int): ClaimPath = append(Segment.Index(value))

    /** Append a wildcard segment (all elements of the array here). */
    fun appendWildcard(): ClaimPath = append(Segment.Wildcard)

    /** A single segment of a [ClaimPath]. */
    sealed interface Segment {
        /** A JSON object key, or an mdoc namespace/element name. */
        @Serializable
        data class Key(val name: String) : Segment

        /** A non-negative array index. */
        @Serializable
        data class Index(val value: Int) : Segment {
            init { require(value >= 0) { "ClaimPath array index must be non-negative (got $value)" } }
        }

        /** Every element of the array here (JSON `null` on the wire). */
        @Serializable
        data object Wildcard : Segment
    }

    companion object {
        /**
         * Build a path of only key segments, e.g. `ofKeys(namespace, dataElement)` for mdoc or
         * `ofKeys("address", "locality")` for JSON.
         */
        fun ofKeys(vararg names: String): ClaimPath {
            require(names.isNotEmpty()) { "ClaimPath must be non-empty (OpenID4VP §7)" }
            return ClaimPath(names.map(Segment::Key))
        }

        /** Build a single-key path. */
        fun key(name: String): ClaimPath = ClaimPath(listOf(Segment.Key(name)))
    }
}

/**
 * Reads/writes [ClaimPath] as the OpenID4VP §7 JSON array (strings, non-negative integers, `null`).
 * JSON only. Anything else, or a negative integer, throws.
 */
internal object ClaimPathSerializer : KSerializer<ClaimPath> {

    override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ClaimPath) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ClaimPath can only be serialized to JSON")
        val array = JsonArray(
            value.segments.map { seg ->
                when (seg) {
                    is ClaimPath.Segment.Key -> JsonPrimitive(seg.name)
                    is ClaimPath.Segment.Index -> JsonPrimitive(seg.value)
                    ClaimPath.Segment.Wildcard -> JsonNull
                }
            }
        )
        jsonEncoder.encodeJsonElement(array)
    }

    override fun deserialize(decoder: Decoder): ClaimPath {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ClaimPath can only be deserialized from JSON")
        val array = jsonDecoder.decodeJsonElement() as? JsonArray
            ?: error("ClaimPath must be a JSON array")
        require(array.isNotEmpty()) { "ClaimPath must be non-empty (OpenID4VP §7)" }
        return ClaimPath(array.map { it.toSegment() })
    }

    private fun JsonElement.toSegment(): ClaimPath.Segment = when {
        this is JsonNull -> ClaimPath.Segment.Wildcard
        this is JsonPrimitive && isString -> ClaimPath.Segment.Key(content)
        this is JsonPrimitive -> {
            val asInt = content.toIntOrNull()
                ?: error("ClaimPath integer segment must fit in Int (got '$content')")
            ClaimPath.Segment.Index(asInt) // negativity is rejected by Segment.Index.init
        }
        else -> error("ClaimPath segment must be a JSON string, integer, or null (got: $this)")
    }
}
