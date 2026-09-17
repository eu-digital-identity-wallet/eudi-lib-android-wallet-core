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
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.documenttype.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests reading the transaction data a presentation recorded in the transaction log back to the
 * payload of the type that declared it.
 */
class TransactionalDataExtensionsTest {

    @Test
    fun `a recorded entry is read with the parser of its type`() {
        val payloads = recorded(QES_APPROVAL).payloads(listOf(TransactionDataType.QES_APPROVAL))

        val approval = assertIs<QesApprovalRequest>(payloads.single())
        assertEquals("GX0112348", approval.credentialId)
    }

    @Test
    fun `no accepted type reads nothing back`() {
        val payloads = recorded(QES_APPROVAL).payloads(emptyList())

        assertIs<JsonObject>(payloads.single())
    }

    @Test
    fun `a type a consumer declared is read with its own parser`() {
        val payloads = recorded(PAYMENT).payloads(listOf(TransactionDataType(PaymentTransactionType)))

        val payment = assertIs<Payment>(payloads.single())
        assertEquals("42.00", payment.amount)
    }

    @Test
    fun `an entry of a type that is not declared comes back as it was recorded`() {
        val payloads = recorded(PAYMENT).payloads(listOf(TransactionDataType.QES_APPROVAL))

        val unread = assertIs<JsonObject>(payloads.single())
        assertEquals("https://example.com/2026/payment", unread.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `an entry that no longer conforms to its type comes back as it was recorded`() {
        val malformed = """{"type":"${QesApprovalRequest.TYPE}","credential_ids":["query_0"]}"""

        val payloads = recorded(malformed).payloads(listOf(TransactionDataType.QES_APPROVAL))

        assertIs<JsonObject>(payloads.single())
    }

    @Test
    fun `every recorded entry is returned, in the order it was recorded`() {
        val payloads = recorded(QES_APPROVAL, PAYMENT).payloads(
            listOf(TransactionDataType.QES_APPROVAL, TransactionDataType(PaymentTransactionType))
        )

        assertEquals(2, payloads.size)
        assertIs<QesApprovalRequest>(payloads[0])
        assertIs<Payment>(payloads[1])
    }

    @Test
    fun `an entry without a type comes back as it was recorded`() {
        val payloads = recorded("""{"amount":"42.00"}""").payloads(
            listOf(TransactionDataType.QES_APPROVAL)
        )

        assertIs<JsonObject>(payloads.single())
    }

    private fun recorded(vararg json: String): TransactionalData = TransactionalData(
        JsonArray(json.map { Json.parseToJsonElement(it) })
    )

    @Serializable
    data class Payment(
        @SerialName("type") val type: String,
        @SerialName("credential_ids") val credentialIds: List<String>,
        @SerialName("amount") val amount: String,
    )

    object PaymentTransactionType : TransactionType<Payment>(
        displayName = "Payment",
        identifier = "https://example.com/2026/payment",
    ) {
        override fun parseOpenId4VpRequest(jsonString: String): Payment =
            Json.decodeFromString(Payment.serializer(), jsonString)
    }

    private companion object {
        const val QES_APPROVAL = """{"type":"https://cloudsignatureconsortium.org/2025/qes-approval",""" +
                """"credential_ids":["query_0"],"credentialID":"GX0112348","numSignatures":1,""" +
                """"documentDigests":[{"hash":"sTOgwOm+474gFj0q0x1iSNspKqbcse4IeiqlDg/HWuI="}],""" +
                """"hashAlgorithmOID":"2.16.840.1.101.3.4.2.1"}"""

        const val PAYMENT = """{"type":"https://example.com/2026/payment",""" +
                """"credential_ids":["query_0"],"amount":"42.00"}"""
    }
}
