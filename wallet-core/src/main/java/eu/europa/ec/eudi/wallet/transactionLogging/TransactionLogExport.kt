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

package eu.europa.ec.eudi.wallet.transactionLogging

import android.annotation.SuppressLint
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A simple export of a [TransactionEntry] to JSON.
 */
@SuppressLint("UnsafeOptInUsageError")
object TransactionEntryExportSerializer :
    JsonTransformingSerializer<TransactionEntry>(TransactionEntry.serializer()) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        val source = element.jsonObject
        return buildJsonObject {
            for ((key, value) in source) {
                when (key) {
                    "transactionResult" -> putTransactionResult(value)
                    "details" -> for ((detailKey, detailValue) in value.jsonObject) {
                        put(detailKey, detailValue)
                    }
                    "time" -> put("time", JsonPrimitive(reformatTime(value)))
                    else -> put(key, value)
                }
            }
        }
    }

    /** Turns the stored result object into a `transactionResult` string plus an optional `reasonOfNoncompletion`. */
    private fun JsonObjectBuilder.putTransactionResult(value: JsonElement) {
        val resultObject = value.jsonObject
        val type = resultObject["type"]?.jsonPrimitive?.content ?: return
        put("transactionResult", JsonPrimitive(type))
        resultObject["reason"]
            ?.takeUnless { it is JsonNull }
            ?.let { put("reasonOfNoncompletion", it) }
    }

    /** Reformats a stored ISO-8601 time to a UTC instant truncated to seconds, keeping the `Z` (e.g. `...20Z`). */
    private fun reformatTime(value: JsonElement): String {
        val raw = value.jsonPrimitive.content
        return runCatching {
            DateTimeFormatter.ISO_INSTANT.format(Instant.parse(raw).truncatedTo(ChronoUnit.SECONDS))
        }.getOrDefault(raw)
    }
}

/**
 * Produces the TS10 §4.1 Transaction Log Object JSON, using [TransactionEntryExportSerializer]
 * for each entry. Null fields are dropped; non-null defaults are kept.
 */
@SuppressLint("UnsafeOptInUsageError")
class TransactionLogExport {

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    /** Serializes a single [TransactionEntry] to its TS10 §4.1 wire-shape JSON object string. */
    fun encodeEntry(entry: TransactionEntry): String =
        json.encodeToString(TransactionEntryExportSerializer, entry)

    /** Serializes a single [TransactionEntry] to its TS10 §4.1 wire-shape [JsonObject]. */
    fun encodeEntryToJsonObject(entry: TransactionEntry): JsonObject =
        json.encodeToJsonElement(TransactionEntryExportSerializer, entry).jsonObject

    /**
     * Serializes a list of entries to the TS10 §4.1 Transaction Log Object:
     * `{"TransactionLog": [ <entry>, ... ]}`.
     */
    fun encode(entries: List<TransactionEntry>): String {
        val array = JsonArray(entries.map { encodeEntryToJsonObject(it) })
        val root = buildJsonObject { put(TRANSACTION_LOG_KEY, array) }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    companion object {
        const val TRANSACTION_LOG_KEY = "TransactionLog"
    }
}
