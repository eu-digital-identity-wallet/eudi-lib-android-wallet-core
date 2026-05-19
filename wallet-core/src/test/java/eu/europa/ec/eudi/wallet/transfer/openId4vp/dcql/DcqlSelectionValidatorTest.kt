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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.openid4vp.dcql.ClaimId
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimSet
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQueryIds
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSetQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSets
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaSdJwtVcExtensions
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.multipaz.openid.dcql.DcqlClaimSet
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.RequestedClaim
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [validateSelection]. Covers credential-coverage and claim-completeness
 * checks against the original DCQL request after consent-UI changes.
 */
class DcqlSelectionValidatorTest {

    /** Single required query with all claims disclosed → valid. */
    @Test
    fun `single required query with all claims is valid`() {
        val pidId = QueryId("pid")
        val claimGivenName = jsonClaim("given_name")
        val claimFamilyName = jsonClaim("family_name")

        val dcql = dcql(
            CredentialQuery.sdJwtVc(
                id = pidId,
                sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
                requireCryptographicHolderBinding = false,
                claims = listOf(
                    ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("given_name")))),
                    ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("family_name")))),
                ),
            ),
        )

        val selection = CredentialPresentmentSelection(
            matches = listOf(matchFor(pidId, requiredClaims = listOf(claimGivenName, claimFamilyName))),
        )

        assertNull(validateSelection(selection, dcql))
    }

    /** Required credential missing entirely → error. */
    @Test
    fun `required credential missing is rejected`() {
        val pidId = QueryId("pid")

        val dcql = dcql(
            CredentialQuery.sdJwtVc(
                id = pidId,
                sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
                requireCryptographicHolderBinding = false,
                claims = listOf(
                    ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("given_name")))),
                ),
            ),
        )

        val selection = CredentialPresentmentSelection(matches = emptyList())

        val error = validateSelection(selection, dcql)
        assertNotNull(error)
        assertTrue(error.contains("pid"), "Expected error to mention the missing query id; got: $error")
    }

    /** A single required claim removed from the disclosed set → error. */
    @Test
    fun `match with missing required claim is rejected`() {
        val pidId = QueryId("pid")
        val claimGivenName = jsonClaim("given_name")
        val claimFamilyName = jsonClaim("family_name")

        val dcql = dcql(
            CredentialQuery.sdJwtVc(
                id = pidId,
                sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
                requireCryptographicHolderBinding = false,
                claims = listOf(
                    ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("given_name")))),
                    ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("family_name")))),
                ),
            ),
        )

        // The user kept only given_name; family_name was deselected.
        val selection = CredentialPresentmentSelection(
            matches = listOf(
                matchFor(
                    queryId = pidId,
                    requiredClaims = listOf(claimGivenName, claimFamilyName),
                    disclosedClaims = listOf(claimGivenName),
                ),
            ),
        )

        val error = validateSelection(selection, dcql)
        assertNotNull(error)
        assertTrue(error.contains("pid"), "Expected the error to mention the offending query id; got: $error")
    }

    /** claim_sets alternatives: first set's claim removed but second set's claim disclosed → valid. */
    @Test
    fun `claim_sets alternative still satisfied passes validation`() {
        val pidId = QueryId("pid")
        val claimAgeOver18 = jsonClaim("age_over_18", id = "age_over_18")
        val claimBirthDate = jsonClaim("birth_date", id = "birth_date")

        val dcql = dcql(
            CredentialQuery.sdJwtVc(
                id = pidId,
                sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
                requireCryptographicHolderBinding = false,
                claims = listOf(
                    ClaimsQuery.sdJwtVc(
                        id = ClaimId("age_over_18"),
                        path = ClaimPath(listOf(ClaimPathElement.Claim("age_over_18"))),
                    ),
                    ClaimsQuery.sdJwtVc(
                        id = ClaimId("birth_date"),
                        path = ClaimPath(listOf(ClaimPathElement.Claim("birth_date"))),
                    ),
                ),
                claimSets = listOf(
                    ClaimSet(listOf(ClaimId("age_over_18"))),
                    ClaimSet(listOf(ClaimId("birth_date"))),
                ),
            ),
        )

        // The user deselected age_over_18 but kept birth_date — the second claim_set is satisfied.
        val selection = CredentialPresentmentSelection(
            matches = listOf(
                matchFor(
                    queryId = pidId,
                    requiredClaims = listOf(claimAgeOver18, claimBirthDate),
                    disclosedClaims = listOf(claimBirthDate),
                    queryClaimSets = listOf(
                        dcqlClaimSet("age_over_18"),
                        dcqlClaimSet("birth_date"),
                    ),
                ),
            ),
        )

        assertNull(validateSelection(selection, dcql))
    }

    /** claim_sets alternatives: no set is satisfied after deselection → error. */
    @Test
    fun `claim_sets with no satisfied alternative is rejected`() {
        val pidId = QueryId("pid")
        val claimAgeOver18 = jsonClaim("age_over_18", id = "age_over_18")
        val claimBirthDate = jsonClaim("birth_date", id = "birth_date")
        val claimUnrelated = jsonClaim("family_name")

        val dcql = dcql(
            CredentialQuery.sdJwtVc(
                id = pidId,
                sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
                requireCryptographicHolderBinding = false,
                claims = listOf(
                    ClaimsQuery.sdJwtVc(
                        id = ClaimId("age_over_18"),
                        path = ClaimPath(listOf(ClaimPathElement.Claim("age_over_18"))),
                    ),
                    ClaimsQuery.sdJwtVc(
                        id = ClaimId("birth_date"),
                        path = ClaimPath(listOf(ClaimPathElement.Claim("birth_date"))),
                    ),
                ),
                claimSets = listOf(
                    ClaimSet(listOf(ClaimId("age_over_18"))),
                    ClaimSet(listOf(ClaimId("birth_date"))),
                ),
            ),
        )

        // Neither age_over_18 nor birth_date is in the disclosed set.
        val selection = CredentialPresentmentSelection(
            matches = listOf(
                matchFor(
                    queryId = pidId,
                    requiredClaims = listOf(claimAgeOver18, claimBirthDate),
                    disclosedClaims = listOf(claimUnrelated),
                    queryClaimSets = listOf(
                        dcqlClaimSet("age_over_18"),
                        dcqlClaimSet("birth_date"),
                    ),
                ),
            ),
        )

        assertNotNull(validateSelection(selection, dcql))
    }

    /** Optional credential_set skipped entirely → valid. */
    @Test
    fun `optional credential_set skipped is valid`() {
        val pidId = QueryId("pid")
        val photoId = QueryId("photo")
        val claimGivenName = jsonClaim("given_name")

        val dcql = dcql(
            credentials = listOf(
                CredentialQuery.sdJwtVc(
                    id = pidId,
                    sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
                    requireCryptographicHolderBinding = false,
                    claims = listOf(
                        ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("given_name")))),
                    ),
                ),
                CredentialQuery.sdJwtVc(
                    id = photoId,
                    sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eu:photoid:1")),
                    requireCryptographicHolderBinding = false,
                    claims = listOf(
                        ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("portrait")))),
                    ),
                ),
            ),
            credentialSets = listOf(
                CredentialSetQuery(options = listOf(CredentialQueryIds(listOf(pidId))), required = true),
                CredentialSetQuery(options = listOf(CredentialQueryIds(listOf(photoId))), required = false),
            ),
        )

        // Only the required set has a match; the optional photo set is left empty.
        val selection = CredentialPresentmentSelection(
            matches = listOf(matchFor(pidId, requiredClaims = listOf(claimGivenName))),
        )

        assertNull(validateSelection(selection, dcql))
    }

    /** Required credential_set: no option satisfied → error. */
    @Test
    fun `required credential_set with no option satisfied is rejected`() {
        val mdlId = QueryId("mdl")
        val pidId = QueryId("pid")
        val claimGivenName = jsonClaim("given_name")

        val dcql = dcql(
            credentials = listOf(
                CredentialQuery.sdJwtVc(
                    id = mdlId,
                    sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:mdl:1")),
                    requireCryptographicHolderBinding = false,
                    claims = listOf(
                        ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("given_name")))),
                    ),
                ),
                CredentialQuery.sdJwtVc(
                    id = pidId,
                    sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
                    requireCryptographicHolderBinding = false,
                    claims = listOf(
                        ClaimsQuery.sdJwtVc(path = ClaimPath(listOf(ClaimPathElement.Claim("given_name")))),
                    ),
                ),
            ),
            credentialSets = listOf(
                // Required set: one of [mdl] or [pid] must be present. Neither is.
                CredentialSetQuery(
                    options = listOf(
                        CredentialQueryIds(listOf(mdlId)),
                        CredentialQueryIds(listOf(pidId)),
                    ),
                    required = true,
                ),
            ),
        )

        val selection = CredentialPresentmentSelection(matches = emptyList())

        val error = validateSelection(selection, dcql)
        assertNotNull(error)
        assertTrue(
            error.contains("credential_set"),
            "Expected the error to mention the unsatisfied set; got: $error",
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Build a single-query DCQL with no credential_sets — implicit all-required. */
    private fun dcql(query: CredentialQuery): DCQL =
        DCQL(credentials = Credentials(listOf(query)), credentialSets = null)

    private fun dcql(
        credentials: List<CredentialQuery>,
        credentialSets: List<CredentialSetQuery>?,
    ): DCQL = DCQL(
        credentials = Credentials(credentials),
        credentialSets = credentialSets?.let(::CredentialSets),
    )

    /**
     * Build a real [JsonRequestedClaim] for an SD-JWT VC top-level claim path. Data
     * class equality lets the same claim be referenced in both the query's claim list
     * and the match's disclosed set.
     */
    private fun jsonClaim(name: String, id: String? = null): JsonRequestedClaim =
        JsonRequestedClaim(
            id = id,
            vctValues = listOf("urn:test"),
            claimPath = JsonArray(listOf(JsonPrimitive(name))),
        )

    /**
     * Build a match whose source reports [queryId]. [requiredClaims] populates the
     * source query's `claims` (what the verifier asked for); [disclosedClaims]
     * populates the match's `claims` map (what the consent UI will share — usually
     * the same as required, but tests for deselection pass a smaller list).
     */
    private fun matchFor(
        queryId: QueryId,
        requiredClaims: List<RequestedClaim>,
        disclosedClaims: List<RequestedClaim> = requiredClaims,
        queryClaimSets: List<DcqlClaimSet> = emptyList(),
    ): CredentialPresentmentSetOptionMemberMatch {
        val credentialQuery = mockk<DcqlCredentialQuery>(relaxed = true) {
            every { id } returns queryId.value
            every { claims } returns requiredClaims
            every { claimSets } returns queryClaimSets
        }
        val claimsMap: Map<RequestedClaim, org.multipaz.claim.Claim> =
            disclosedClaims.associateWith { mockk(relaxed = true) }
        return mockk {
            every { source } returns CredentialMatchSourceOpenID4VP(credentialQuery = credentialQuery)
            every { claims } returns claimsMap
        }
    }

    /** Build a `DcqlClaimSet` from a list of claim ids. */
    private fun dcqlClaimSet(vararg claimIds: String): DcqlClaimSet = mockk(relaxed = true) {
        every { claimIdentifiers } returns claimIds.toList()
    }
}
