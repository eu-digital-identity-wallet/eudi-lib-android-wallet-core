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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation

import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.claim.Claim
import org.multipaz.credential.Credential
import org.multipaz.mdoc.request.DocRequest
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.presentment.CredentialMatchSource
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests how [PresentationLogBuilder.withRequest] fills `listOfClaimsRequested` (TS10 §3.2) from the
 * matched claims of the processed request, per credential, paths only.
 */
class PresentationLogBuilderRequestedClaimsTest {

    private val builder = PresentationLogBuilder()

    @Test
    fun `mdoc device request populates listOfClaimsRequested with matched namespace-element paths`() {
        val source = mdocSource(docType = "org.iso.18013.5.1.mDL")
        val match = matchOf(
            source = source,
            claims = listOf(
                mdocClaim("org.iso.18013.5.1.mDL", "org.iso.18013.5.1", "family_name"),
                mdocClaim("org.iso.18013.5.1.mDL", "org.iso.18013.5.1", "given_name"),
            ),
        )
        val request = DeviceRequest(deviceRequestBytes = byteArrayOf(), sessionTranscriptBytes = byteArrayOf())

        val log = builder.withRequest(builder.createEmptyPresentationLog(), request, success(treeOf(match)))

        val claim = log.listOfClaimsRequested.single()
        assertEquals("org.iso.18013.5.1.mDL", claim.credentialIdentifier)
        assertEquals(
            listOf(
                ClaimPath.ofKeys("org.iso.18013.5.1", "family_name"),
                ClaimPath.ofKeys("org.iso.18013.5.1", "given_name"),
            ),
            claim.claims,
        )
    }

    @Test
    fun `openid4vp sd-jwt request populates listOfClaimsRequested with vct and matched claim paths`() {
        val source = sdJwtSource("eu.europa.ec.eudi.pid.1")
        val match = matchOf(
            source = source,
            claims = listOf(
                jsonClaim("eu.europa.ec.eudi.pid.1", JsonArray(listOf(JsonPrimitive("family_name")))),
                jsonClaim(
                    "eu.europa.ec.eudi.pid.1",
                    JsonArray(listOf(JsonPrimitive("address"), JsonPrimitive("locality"))),
                ),
            ),
        )
        val request = OpenId4VpRequest(mockk(relaxed = true))

        val log = builder.withRequest(builder.createEmptyPresentationLog(), request, success(treeOf(match)))

        val claim = log.listOfClaimsRequested.single()
        assertEquals("eu.europa.ec.eudi.pid.1", claim.credentialIdentifier)
        assertEquals(
            listOf(ClaimPath.ofKeys("family_name"), ClaimPath.ofKeys("address", "locality")),
            claim.claims,
        )
    }

    @Test
    fun `only matched claims are logged, not the full request`() {
        // The verifier may ask for more, but only the matched claim is logged.
        val source = mdocSource(docType = "org.iso.18013.5.1.mDL")
        val match = matchOf(
            source = source,
            claims = listOf(mdocClaim("org.iso.18013.5.1.mDL", "org.iso.18013.5.1", "family_name")),
        )
        val request = DeviceRequest(byteArrayOf(), byteArrayOf())

        val log = builder.withRequest(builder.createEmptyPresentationLog(), request, success(treeOf(match)))

        val claim = log.listOfClaimsRequested.single()
        assertEquals(listOf(ClaimPath.ofKeys("org.iso.18013.5.1", "family_name")), claim.claims)
    }

    @Test
    fun `multiple candidate matches for the same credential are de-duplicated`() {
        val source = mdocSource(docType = "org.iso.18013.5.1.mDL")
        val claims = listOf(mdocClaim("org.iso.18013.5.1.mDL", "org.iso.18013.5.1", "family_name"))
        // Two candidate credentials, so two matches sharing the same DocRequest source.
        val request = DeviceRequest(byteArrayOf(), byteArrayOf())

        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            request,
            success(treeOf(matchOf(source, claims), matchOf(source, claims))),
        )

        val claim = log.listOfClaimsRequested.single()
        assertEquals(listOf(ClaimPath.ofKeys("org.iso.18013.5.1", "family_name")), claim.claims)
    }

    @Test
    fun `unsupported request type marks the entry not completed`() {
        val request = object : Request {}

        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            request,
            success(CredentialPresentmentData(emptyList())),
        )

        val result = assertIs<TransactionResult.NotCompleted>(log.transactionResult)
        assertEquals("Unsupported request type", result.reason)
        assertEquals(emptyList(), log.listOfClaimsRequested)
    }

    @Test
    fun `a request the wallet cannot satisfy is marked not completed with a specific reason`() {
        val request = DeviceRequest(byteArrayOf(), byteArrayOf())

        val log = builder.withRequest(
            builder.createEmptyPresentationLog(),
            request,
            success(CredentialPresentmentData(emptyList())),
        )

        val result = assertIs<TransactionResult.NotCompleted>(log.transactionResult)
        assertEquals(PresentationLogBuilder.REASON_REQUEST_NOT_SATISFIABLE, result.reason)
        assertEquals(emptyList(), log.listOfClaimsRequested)
    }

    @Test
    fun `a failed processed request records the underlying error as the reason`() {
        val request = DeviceRequest(byteArrayOf(), byteArrayOf())
        val failure = RequestProcessor.ProcessedRequest.Failure(
            IllegalStateException("Transaction data is not supported")
        )

        val log = builder.withRequest(builder.createEmptyPresentationLog(), request, failure)

        val result = assertIs<TransactionResult.NotCompleted>(log.transactionResult)
        assertEquals("Transaction data is not supported", result.reason)
    }

    // ----- helpers ---------------------------------------------------------------------------

    private fun success(data: CredentialPresentmentData): RequestProcessor.ProcessedRequest.Success {
        val processed = mockk<RequestProcessor.ProcessedRequest.Success>()
        every { processed.presentmentData } returns data
        // withRequest reads presentmentSelections; derive it from the tree, like the real getter.
        every { processed.presentmentSelections } returns data.getAllSelections()
        every { processed.getOrNull() } returns processed
        return processed
    }

    private fun matchOf(
        source: CredentialMatchSource,
        claims: List<RequestedClaim>,
    ): CredentialPresentmentSetOptionMemberMatch =
        CredentialPresentmentSetOptionMemberMatch(
            credential = mockk<Credential>(relaxed = true),
            claims = claims.associateWith { mockk<Claim>(relaxed = true) },
            source = source,
            transactionData = emptyList(),
        )

    private fun treeOf(vararg matches: CredentialPresentmentSetOptionMemberMatch): CredentialPresentmentData =
        CredentialPresentmentData(
            listOf(
                CredentialPresentmentSet(
                    optional = false,
                    options = listOf(
                        CredentialPresentmentSetOption(
                            members = listOf(CredentialPresentmentSetOptionMember(matches = matches.toList())),
                        ),
                    ),
                ),
            ),
        )

    private fun mdocSource(docType: String): CredentialMatchSourceIso18013 {
        val docRequest = mockk<DocRequest>()
        every { docRequest.docType } returns docType
        return CredentialMatchSourceIso18013(docRequest = docRequest)
    }

    private fun sdJwtSource(vct: String): CredentialMatchSourceOpenID4VP {
        val query = mockk<DcqlCredentialQuery>()
        every { query.id } returns "query-$vct"
        every { query.mdocDocType } returns null
        every { query.vctValues } returns listOf(vct)
        return CredentialMatchSourceOpenID4VP(credentialQuery = query)
    }

    private fun mdocClaim(docType: String, namespace: String, element: String): MdocRequestedClaim =
        MdocRequestedClaim(
            id = null,
            docType = docType,
            namespaceName = namespace,
            dataElementName = element,
            intentToRetain = false,
            values = null,
        )

    private fun jsonClaim(vct: String, claimPath: JsonArray): JsonRequestedClaim =
        JsonRequestedClaim(
            id = null,
            vctValues = listOf(vct),
            claimPath = claimPath,
            values = null,
        )
}
