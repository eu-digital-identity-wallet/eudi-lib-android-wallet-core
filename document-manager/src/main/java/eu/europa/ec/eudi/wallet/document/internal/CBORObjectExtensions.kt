/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.wallet.document.internal

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import java.util.Base64

/**
 * Decodes a ByteArray to a CBORObject and extracts any embedded CBOR object.
 *
 * @return The decoded CBOR object, with any tag-24 wrapping removed.
 */
@JvmSynthetic
internal fun ByteArray.getEmbeddedCBORObject(): CBORObject {
    return CBORObject.DecodeFromBytes(this).getEmbeddedCBORObject()
}

/**
 * Extracts an embedded CBOR object if this object has tag 24.
 * Tag 24 indicates that the byte string contains an encoded CBOR data item.
 *
 * @return The embedded CBOR object if this has tag 24, or this object itself otherwise.
 */
@JvmSynthetic
internal fun CBORObject.getEmbeddedCBORObject(): CBORObject {
    return if (HasTag(24)) {
        CBORObject.DecodeFromBytes(GetByteString())
    } else {
        this
    }
}

/**
 * Wraps a ByteArray with CBOR tag 24 and encodes it.
 * Tag 24 indicates that the byte string contains an encoded CBOR data item.
 *
 * @return The encoded ByteArray with tag 24 applied.
 */
@JvmSynthetic
internal fun ByteArray.withTag24(): ByteArray {
    return CBORObject.FromObjectAndTag(this, 24).EncodeToBytes()
}

/**
 * Converts a CBOR-encoded ByteArray to a native Kotlin object.
 *
 * @return The decoded value as a native Kotlin type (Map, List, String, Number, Boolean, or null).
 */
@JvmSynthetic
internal fun ByteArray.toObject(): Any? {
    return CBORObject.DecodeFromBytes(this).parse()
}

/**
 * Parses a CBORObject to a native Kotlin object.
 *
 * Handles conversion of:
 * - null values
 * - boolean values
 * - numeric values (as Int, Long, or Double)
 * - byte strings (as Base64URL strings or parsed objects if tagged)
 * - text strings
 * - arrays (as Lists)
 * - maps (as Map associations)
 *
 * @return The CBORObject converted to an appropriate native Kotlin type.
 */
private fun CBORObject.parse(): Any? = when {
    isNull -> null
    isTrue -> true
    isFalse -> false
    isNumber -> when {
        CanValueFitInInt32() -> AsInt32Value()
        CanValueFitInInt64() -> AsInt64Value()
        else -> AsDouble()
    }

    else -> when (type) {
        CBORType.ByteString -> when {
            HasMostOuterTag(24) -> GetByteString().toObject()
            else -> Base64.getUrlEncoder().encodeToString(GetByteString())
        }

        CBORType.TextString -> AsString()
        CBORType.Array -> values.map { it.parse() }.toList()
        CBORType.Map -> keys.associate { it.parse() to this[it].parse() }
        else -> null
    }
}

