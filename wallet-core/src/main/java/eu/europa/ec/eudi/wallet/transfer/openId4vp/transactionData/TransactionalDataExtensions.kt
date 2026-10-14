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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData

import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionalData
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads the transaction data a presentation recorded in the transaction log, with the parser of the
 * type that declared it, and returns one payload per recorded entry, in the order they were
 * recorded.
 *
 * The transaction log keeps transaction data as it was received, without its type, because the log
 * is not specific to any type. [types] resolves it again — pass the types the wallet is configured
 * with, so a consumer that declared its own type reads back its own payload, exactly as it received
 * it at presentation time.
 *
 * An entry is returned as the [JsonObject] it was recorded as when no given type claims it, or when
 * it no longer conforms to the type that does, for example after that type's definition changed. A
 * transaction log is read to be displayed, so an entry that cannot be read is returned unread rather
 * than failing the whole log.
 *
 * @param types the transaction data types to read with
 * @return the payload of each recorded entry, or the entry itself when it cannot be read
 */
fun TransactionalData.payloads(types: List<TransactionDataType>): List<Any> =
    content.map { recorded -> recorded.payloadOrNull(types) ?: recorded }

private fun JsonElement.payloadOrNull(types: List<TransactionDataType>): Any? {
    val identifier = (this as? JsonObject)
        ?.get(TRANSACTION_DATA_TYPE)
        ?.jsonPrimitive
        ?.contentOrNull
        ?: return null
    val parser = types.firstOrNull { it.value == identifier }?.parser ?: return null
    return runCatching { parser.parseOpenId4VpRequest(toString()) }.getOrNull()
}

private const val TRANSACTION_DATA_TYPE = "type"
