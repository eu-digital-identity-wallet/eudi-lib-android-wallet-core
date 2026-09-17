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

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.documenttype.TransactionType
import org.multipaz.presentment.TransactionData
import org.multipaz.presentment.TransactionProtocol
import org.multipaz.util.fromBase64Url

/**
 * A transaction data type whose contents are read as plain JSON. Only the members that OpenID4VP
 * itself defines are checked; the rest is presented as it was received.
 *
 * @param identifier the transaction data type identifier
 */
class RawTransactionType(identifier: String) : TransactionType<JsonObject>(
    displayName = identifier,
    identifier = identifier,
) {
    override fun parseOpenId4VpRequest(jsonString: String): JsonObject =
        Json.parseToJsonElement(jsonString).jsonObject

    override fun parseJson(serialized: ByteString): TransactionData<JsonObject> {
        val payload = parseOpenId4VpRequest(
            serialized.decodeToString().fromBase64Url().decodeToString()
        )
        val hashAlgorithms = payload[TRANSACTION_DATA_HASHES_ALG]
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
        return TransactionData(
            type = this,
            payload = payload,
            protocol = TransactionProtocol.OPENID4VP,
            rawBytes = serialized,
            hashAlgorithms = parseJoseHashAlgorithms(hashAlgorithms),
        )
    }

    private companion object {
        const val TRANSACTION_DATA_HASHES_ALG = "transaction_data_hashes_alg"
    }
}
