/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci.tokens

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The alias of the Android Keystore key that encrypts the tokens stored for re-issuance and
 * deferred issuance.
 */
internal const val TOKEN_KEY_ALIAS = "eu.europa.ec.eudi.wallet.issuance.tokens"

/**
 * Serializes the writes of the contexts of deferred documents, so that encrypting a context stored in
 * plain text never overwrites a context stored meanwhile.
 */
internal val deferredContextWriteLock = Mutex()

/** The purposes of the stored records that hold tokens, used when their tokens are sealed. */
internal object TokenRecord {
    const val ISSUANCE_METADATA = "issuance_metadata"
    const val DEFERRED_CONTEXT = "deferred_context"
}

/**
 * Returns the values that the sealed tokens of a stored record are bound to: its [fields], except
 * [unbound]. The fields are taken in the order of their names, and each one gives `@` followed by its
 * name, then the parts of its value: `=` followed by the text of a string, `#` followed by a number or
 * a boolean, `null` for null, and `[` followed by the number of items of a list, then the parts of its
 * items. A string is bound by its text, which does not depend on how it is escaped in the JSON. A
 * number or a boolean is bound by its JSON text, which has one form for integers.
 *
 * Only the fields present in the record are bound. A field added in a later version is therefore bound
 * as well, and a record stored before it was added gives the same values as when it was stored.
 *
 * @throws IllegalArgumentException when a field is an object
 */
internal fun boundValues(fields: JsonObject, unbound: Set<String>): List<String> =
    fields.keys.filter { it !in unbound }.sorted().flatMap { name ->
        listOf("@$name") + boundValue(name, fields.getValue(name))
    }

private fun boundValue(name: String, value: JsonElement): List<String> = when (value) {
    is JsonNull -> listOf("null")
    is JsonPrimitive -> listOf(if (value.isString) "=" + value.content else "#" + value.content)
    is JsonArray -> listOf("[" + value.size) + value.flatMap { boundValue(name, it) }
    is JsonObject -> throw IllegalArgumentException("Field $name cannot be bound")
}

/**
 * JSON for the records that hold tokens. Every field is written, also one with its default value, so
 * that a stored record does not depend on the defaults of the code that reads it. Records are decoded
 * within [decodeTokenRecord].
 */
internal val tokenRecordJson = Json { encodeDefaults = true }

/**
 * Runs [decode] on the stored record of [recordId], which may hold tokens. A decoding error is thrown
 * again as an [UndecodableTokenRecordException] without its cause, with a message that names only
 * [recordId], since the error of the JSON library may quote the input, and so the tokens.
 */
internal inline fun <T> decodeTokenRecord(recordId: String, decode: () -> T): T = try {
    decode()
} catch (e: IllegalArgumentException) {
    throw UndecodableTokenRecordException("The stored record of $recordId cannot be decoded")
}

/**
 * A stored record that holds tokens cannot be decoded, or does not have the fields of such a record.
 * Its message holds no part of the record.
 */
internal class UndecodableTokenRecordException(message: String) : IllegalStateException(message)
