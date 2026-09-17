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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation

import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogExport
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.toJson
import eu.europa.ec.eudi.wallet.transactionLogging.toTransactionEntryOrNull
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalTransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.claim.Claim
import org.multipaz.credential.Credential
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.presentment.CredentialQueryResult
import org.multipaz.presentment.TransactionData
import org.multipaz.request.JsonRequestedClaim
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests how [PresentationLogBuilder.withRequest] fills `transactionalData` (TS10 §3.2, §3.19.12)
 * from the transaction data the presentation request carried.
 */
class PresentationLogBuilderTransactionalDataTest {

    private val builder = PresentationLogBuilder()

    @Test
    fun `a request without transaction data records none`() {
        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            mockk<OpenId4VpRequest>(),
            success(treeOf(matchOf())),
        )

        assertNull(log.transactionalData)
    }

    @Test
    fun `the transaction data is recorded as the request carried it`() {
        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            mockk<OpenId4VpRequest>(),
            success(treeOf(matchOf(QES_APPROVAL))),
        )

        val recorded = log.transactionalData?.content.orEmpty().single().jsonObject
        assertEquals(QesApprovalTransactionType.identifier, recorded.getValue("type").jsonPrimitive.content)
        assertEquals("GX0112348", recorded.getValue("credentialID").jsonPrimitive.content)
        assertEquals(1, assertIs<JsonArray>(recorded.getValue("documentDigests")).size)
    }

    @Test
    fun `several transaction data are recorded in the order they were carried`() {
        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            mockk<OpenId4VpRequest>(),
            success(treeOf(matchOf(QES_APPROVAL), matchOf(OTHER))),
        )

        val recorded = log.transactionalData?.content.orEmpty()
        assertEquals(2, recorded.size)
        assertEquals(
            QesApprovalTransactionType.identifier,
            recorded[0].jsonObject.getValue("type").jsonPrimitive.content,
        )
        assertEquals("https://example.com/2026/other", recorded[1].jsonObject.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `transaction data carried by several credentials is recorded once`() {
        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            mockk<OpenId4VpRequest>(),
            success(treeOf(matchOf(QES_APPROVAL), matchOf(QES_APPROVAL))),
        )

        assertEquals(1, log.transactionalData?.content?.size)
    }

    @Test
    fun `the recorded transaction data survives storage and appears in the export`() {
        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            mockk<OpenId4VpRequest>(),
            success(treeOf(matchOf(QES_APPROVAL))),
        )

        val restored = log.toJson().toTransactionEntryOrNull()
        assertEquals(log.transactionalData, assertIs<TransactionEntry.Presentation>(restored).transactionalData)

        val exported = TransactionLogExport().encodeEntryToJsonObject(log)
        assertEquals(
            QesApprovalTransactionType.identifier,
            assertIs<JsonArray>(exported.getValue("transactionalData"))
                .single().jsonObject.getValue("type").jsonPrimitive.content,
        )
    }

    private fun success(data: CredentialQueryResult): RequestProcessor.ProcessedRequest.Success {
        val processed = mockk<RequestProcessor.ProcessedRequest.Success>()
        every { processed.presentmentData } returns data
        every { processed.presentmentSelections } returns data.getAllSelections()
        every { processed.getOrNull() } returns processed
        return processed
    }

    private var nextQuery = 0

    private fun matchOf(vararg json: String): CredentialPresentmentSetOptionMemberMatch {
        val query = mockk<DcqlCredentialQuery>()
        every { query.id } returns "query_${nextQuery++}"
        every { query.mdocDocType } returns null
        every { query.vctValues } returns listOf(VCT)
        val claim = JsonRequestedClaim(
            id = null,
            vctValues = listOf(VCT),
            claimPath = JsonArray(emptyList()),
            values = null,
        )
        return CredentialPresentmentSetOptionMemberMatch(
            credential = mockk<Credential>(relaxed = true),
            claims = mapOf(claim to mockk<Claim>(relaxed = true)),
            source = CredentialMatchSourceOpenID4VP(credentialQuery = query),
            transactionData = json.map { transactionData(it) },
        )
    }

    /**
     * Wraps [json] the way the processor does: base64url-encoded, so what is recorded comes from
     * the bytes the request carried.
     */
    private fun transactionData(json: String): TransactionData<*> {
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))
        return mockk<TransactionData<*>> {
            every { rawBytes } returns ByteString(encoded.toByteArray(Charsets.US_ASCII))
        }
    }

    private fun treeOf(vararg matches: CredentialPresentmentSetOptionMemberMatch): CredentialQueryResult =
        CredentialQueryResult(
            listOf(
                CredentialPresentmentSet(
                    optional = false,
                    options = listOf(
                        CredentialPresentmentSetOption(
                            members = matches.map { CredentialPresentmentSetOptionMember(matches = listOf(it)) },
                        ),
                    ),
                ),
            ),
        )

    private companion object {
        const val VCT = "urn:eudi:pid:1"

        const val QES_APPROVAL = """{"type":"https://cloudsignatureconsortium.org/2025/qes-approval",""" +
                """"credential_ids":["query_0"],"credentialID":"GX0112348","numSignatures":1,""" +
                """"documentDigests":[{"hash":"sTOgwOm+474gFj0q0x1iSNspKqbcse4IeiqlDg/HWuI="}],""" +
                """"hashAlgorithmOID":"2.16.840.1.101.3.4.2.1"}"""

        const val OTHER = """{"type":"https://example.com/2026/other","credential_ids":["query_1"]}"""
    }
}
