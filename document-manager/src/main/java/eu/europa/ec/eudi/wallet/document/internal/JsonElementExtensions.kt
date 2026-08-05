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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

/**
 * Extension function to parse a [JsonElement] into native Kotlin types.
 *
 * This function recursively converts JSON structures into equivalent Kotlin types:
 * - [JsonArray] is converted to a [List] of parsed elements
 * - [JsonObject] is converted to a [Map] with parsed values
 * - [JsonNull] is converted to `null`
 * - [JsonPrimitive] is converted to the most appropriate Kotlin type:
 *   - String values remain as strings
 *   - Numeric values are converted to Int, Long, Float, or Double based on the content
 *   - Boolean values are converted to Boolean
 *   - If conversion fails, falls back to the original string content
 *
 * @return The parsed value as a Kotlin type, or null if parsing fails
 */
internal fun JsonElement.parse(): Any? {
    return when (this) {
        is JsonArray -> map { it.parse() }
        is JsonObject -> mapValues { (_, v) -> v.parse() }
        is JsonNull -> null
        is JsonPrimitive -> when {
            isString -> runCatching { content }

            else -> runCatching { int }
                .recoverCatching { long }
                .recoverCatching { float }
                .recoverCatching { double }
                .recoverCatching { boolean }
                .recoverCatching { content }

        }.getOrNull()
    }
}