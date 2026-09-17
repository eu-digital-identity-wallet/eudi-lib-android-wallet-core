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

import eu.europa.ec.eudi.wallet.internal.transactionDataKeyBindingClaims
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.crypto.Algorithm
import org.multipaz.presentment.TransactionData
import org.multipaz.presentment.TransactionProtocol
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the `qesApproval` claim with which the QES approval type binds a presentation to the
 * transaction data it approves, as CSC Data Model Bindings clause 7.2.1.2 defines it and OpenID4VP
 * Appendix B.3.3 recommends every transaction data type does.
 */
class QesApprovalKeyBindingTest {

    @Test
    fun `the approval is the base64 digest of the transaction data string as received`() {
        val encoded = encodedApproval()
        val expected = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(encoded.toByteArray(Charsets.US_ASCII))
        )

        val claims = listOf(parse(encoded)).transactionDataKeyBindingClaims()

        assertEquals(JsonPrimitive(expected), claims[QES_APPROVAL_CLAIM])
    }

    @Test
    fun `the approval is not calculated over the decoded transaction data`() {
        val encoded = encodedApproval()
        val overDecoded = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(approvalJson().toByteArray())
        )

        val claims = listOf(parse(encoded)).transactionDataKeyBindingClaims()

        assertTrue(claims[QES_APPROVAL_CLAIM] != JsonPrimitive(overDecoded))
    }

    @Test
    fun `the approval and the profile hash carry the same digest when their algorithms agree`() {
        val claims = listOf(parse(encodedApproval())).transactionDataKeyBindingClaims()

        val approval = Base64.getDecoder()
            .decode(claims.getValue(QES_APPROVAL_CLAIM).jsonPrimitive.content)
        val profile = Base64.getUrlDecoder().decode(
            (claims.getValue("transaction_data_hashes") as JsonArray).single().jsonPrimitive.content
        )

        assertContentEquals(profile, approval)
    }

    @Test
    fun `the approval and the profile hash differ when their algorithms differ`() {
        val claims = listOf(parse(encodedApproval(hashAlgorithmOid = SHA_384_OID)))
            .transactionDataKeyBindingClaims()

        val approval = Base64.getDecoder()
            .decode(claims.getValue(QES_APPROVAL_CLAIM).jsonPrimitive.content)
        val profile = Base64.getUrlDecoder().decode(
            (claims.getValue("transaction_data_hashes") as JsonArray).single().jsonPrimitive.content
        )

        assertEquals(
            "sha-256",
            claims.getValue("transaction_data_hashes_alg").jsonPrimitive.content
        )
        assertFalse(profile.contentEquals(approval))
    }

    @Test
    fun `the approval is calculated with the algorithm the request names`() {
        val encoded = encodedApproval(hashAlgorithmOid = SHA_384_OID)
        val expected = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-384").digest(encoded.toByteArray(Charsets.US_ASCII))
        )

        val claims = listOf(parse(encoded)).transactionDataKeyBindingClaims()

        assertEquals(JsonPrimitive(expected), claims[QES_APPROVAL_CLAIM])
    }

    @Test
    fun `a hash algorithm the wallet cannot use is rejected`() {
        val encoded = encodedApproval(hashAlgorithmOid = "1.3.36.3.2.1")

        assertFailsWith<IllegalArgumentException> {
            listOf(parse(encoded)).transactionDataKeyBindingClaims()
        }
    }

    @Test
    fun `two approvals of one presentation are rejected, since the claim holds one`() {
        val approvals = listOf(parse(encodedApproval()), parse(encodedApproval("Annex")))

        val error = assertFailsWith<IllegalArgumentException> {
            approvals.transactionDataKeyBindingClaims()
        }

        assertTrue(error.message.orEmpty().contains(QES_APPROVAL_CLAIM))
    }

    @Test
    fun `a type that defines no claim of its own adds none`() {
        val claims = listOf(raw()).transactionDataKeyBindingClaims()

        assertNull(claims[QES_APPROVAL_CLAIM])
        assertEquals(
            setOf("transaction_data_hashes", "transaction_data_hashes_alg"),
            claims.keys
        )
    }

    private fun approvalJson(label: String = "Contract", hashAlgorithmOid: String = SHA_256_OID) =
        """{"type":"${QesApprovalRequest.TYPE}","credential_ids":["query_0"],""" +
            """"credentialID":"GX0112348","numSignatures":1,"documentDigests":""" +
            """[{"label":"$label","hash":"sTOgwOm+474gFj0q0x1iSNspKqbcse4IeiqlDg/HWuI="}],""" +
            """"hashAlgorithmOID":"$hashAlgorithmOid"}"""

    private fun encodedApproval(
        label: String = "Contract",
        hashAlgorithmOid: String = SHA_256_OID
    ): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(approvalJson(label, hashAlgorithmOid).toByteArray())

    private fun parse(encoded: String): TransactionData<QesApprovalRequest> =
        QesApprovalTransactionType.parseJson(ByteString(encoded.toByteArray(Charsets.US_ASCII)))

    private fun raw(): TransactionData<JsonObject> = TransactionData(
        type = RawTransactionType("https://example.com/2026/test"),
        payload = JsonObject(emptyMap()),
        protocol = TransactionProtocol.OPENID4VP,
        rawBytes = ByteString("eyJhIjoxfQ".toByteArray(Charsets.US_ASCII)),
        hashAlgorithms = listOf(Algorithm.SHA256),
    )

    private companion object {
        const val QES_APPROVAL_CLAIM = "org.cloudsignatureconsortium.dm.1.qesApproval"
        const val SHA_256_OID = "2.16.840.1.101.3.4.2.1"
        const val SHA_384_OID = "2.16.840.1.101.3.4.2.2"
    }
}
