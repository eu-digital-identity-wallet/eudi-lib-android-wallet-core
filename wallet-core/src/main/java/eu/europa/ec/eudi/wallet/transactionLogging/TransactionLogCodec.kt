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
import kotlinx.serialization.json.Json

/**
 * JSON used to save and load [TransactionEntry] for storage. This form round-trips (read back
 * exactly what you wrote), unlike [TransactionLogExport] which is export-only.
 *
 * Unknown keys are ignored so reads keep working when new fields are added, and defaults are
 * written out so they survive a round-trip.
 */
@SuppressLint("UnsafeOptInUsageError")
private val transactionLogStorageJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Converts this [TransactionEntry] to its storage JSON string. */
@SuppressLint("UnsafeOptInUsageError")
fun TransactionEntry.toJson(): String =
    transactionLogStorageJson.encodeToString(TransactionEntry.serializer(), this)

/**
 * Reads a [TransactionEntry] from its storage JSON string, or returns `null` if it can't be read,
 * so one bad entry can be skipped instead of failing a whole batch.
 */
@SuppressLint("UnsafeOptInUsageError")
fun String.toTransactionEntryOrNull(): TransactionEntry? =
    runCatching {
        transactionLogStorageJson.decodeFromString(TransactionEntry.serializer(), this)
    }.getOrNull()
