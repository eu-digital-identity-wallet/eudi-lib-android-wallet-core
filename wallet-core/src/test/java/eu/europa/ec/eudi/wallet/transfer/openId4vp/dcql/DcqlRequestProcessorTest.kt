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

import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.dcql.ClaimId
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimSet
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaMsoMdocExtensions
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaSdJwtVcExtensions
import eu.europa.ec.eudi.openid4vp.dcql.MsoMdocDocType
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test
import org.multipaz.cbor.Tstr
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.claim.MdocClaim
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests for [DcqlRequestProcessor.process] focused on **hard-match**
 * semantics required by OpenID4VP §6.4.1:
 *
 *  > If the Wallet cannot deliver all claims requested by the Verifier according to
 *  > these rules, it MUST NOT return the respective Credential.
 *
 * Concretely: when a `CredentialQuery` enumerates N claims and a candidate credential
 * in the wallet's store is missing any one of them, that credential must be excluded
 * from the resulting [CredentialPresentmentData] tree — not surfaced with a partial
 * set of disclosures.
 *
 * The companion positive test builds the same DCQL query against a credential that
 * holds all three claims; that credential must surface as a match.
 */
class DcqlRequestProcessorTest {

    /**
     * Mirrors a real-world DCQL request observed in the wallet app: an `mso_mdoc` mDL
     * credential is asked for `family_name`, `given_name` **and** `age_over_21`, with no
     * `claim_sets` (so all three are required per §6.4.1).
     *
     * The wallet holds an mDL credential carrying only `family_name` and `given_name` —
     * `age_over_21` is absent. The processor must therefore produce no matches and the
     * resulting presentment tree must be empty.
     */
    @Test
    fun `mdoc credential missing one of three required claims produces no matches`(): Unit = runBlocking {
        val docType = "org.iso.18013.5.1.mDL"
        val namespace = "org.iso.18013.5.1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.mdoc(
                        id = QueryId("query_0"),
                        msoMdocMeta = DCQLMetaMsoMdocExtensions(
                            doctypeValue = MsoMdocDocType(docType),
                        ),
                        claims = listOf(
                            ClaimsQuery.mdoc(namespace = namespace, claimName = "family_name", intentToRetain = false),
                            ClaimsQuery.mdoc(namespace = namespace, claimName = "given_name", intentToRetain = false),
                            // age_over_21 is required by the verifier, but the wallet's
                            // credential below intentionally omits it.
                            ClaimsQuery.mdoc(namespace = namespace, claimName = "age_over_21", intentToRetain = false),
                        ),
                    ),
                ),
            ),
            // No claim_sets — per §6.4.1 every claim above is required.
            credentialSets = null,
        )

        val credentialClaims = listOf(
            mdocClaim(docType, namespace, dataElementName = "family_name", value = "Doe"),
            mdocClaim(docType, namespace, dataElementName = "given_name", value = "John"),
            // Note: NO age_over_21 — the credential physically lacks this element.
        )
        val processor = buildProcessor(
            credentialFormat = MsoMdocFormat(docType),
            credentialClaims = credentialClaims,
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        // Must surface as a Success (the request itself is valid) — but the presentment
        // tree must be empty because no candidate credential satisfies all claims.
        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertTrue(
            success.presentmentData.credentialSets.isEmpty(),
            "Per OpenID4VP §6.4.1 MUST NOT, a credential missing any requested claim must " +
                "not surface in the presentment tree. Found ${success.presentmentData.credentialSets.size} sets.",
        )
    }

    /**
     * Positive control: the same DCQL request, but the wallet's credential carries **all**
     * three requested claims. The processor must surface this credential as a match in the
     * resulting presentment tree.
     */
    @Test
    fun `mdoc credential carrying all three required claims surfaces as a match`(): Unit = runBlocking {
        val docType = "org.iso.18013.5.1.mDL"
        val namespace = "org.iso.18013.5.1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.mdoc(
                        id = QueryId("query_0"),
                        msoMdocMeta = DCQLMetaMsoMdocExtensions(MsoMdocDocType(docType)),
                        claims = listOf(
                            ClaimsQuery.mdoc(namespace = namespace, claimName = "family_name"),
                            ClaimsQuery.mdoc(namespace = namespace, claimName = "given_name"),
                            ClaimsQuery.mdoc(namespace = namespace, claimName = "age_over_21"),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf(
            mdocClaim(docType, namespace, "family_name", value = "Doe"),
            mdocClaim(docType, namespace, "given_name", value = "John"),
            // age_over_21 is present this time → all three required claims are satisfied.
            mdocClaim(docType, namespace, "age_over_21", value = "true"),
        )
        val processor = buildProcessor(
            credentialFormat = MsoMdocFormat(docType),
            credentialClaims = credentialClaims,
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertTrue(
            success.presentmentData.credentialSets.isNotEmpty(),
            "Credential satisfying all three claims must surface in the presentment tree",
        )
    }

    /**
     * `claim_sets` (OpenID4VP §6.4.1) lets the verifier list **alternative** claim
     * selections from the same credential, in **preference order**. The processor must
     * pick the **first** set whose every claim resolves against the wallet's credential,
     * and the resulting match's `claims` map must contain **only** that set's claims —
     * over-disclosing claims belonging to lower-preference sets is a spec violation and
     * a privacy bug.
     *
     * Setup: an mDL credential carries both `age_over_18` and `birth_date`. The
     * verifier lists them as alternatives via `claim_sets = [["age_over_18"],
     * ["birth_date"]]`. The processor must pick the first set, so the resulting match
     * must surface `age_over_18` only — never `birth_date`.
     */
    @Test
    fun `mdoc claim_sets first satisfied set wins, no over-disclosure`(): Unit = runBlocking {
        val docType = "org.iso.18013.5.1.mDL"
        val namespace = "org.iso.18013.5.1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.mdoc(
                        id = QueryId("query_0"),
                        msoMdocMeta = DCQLMetaMsoMdocExtensions(MsoMdocDocType(docType)),
                        claims = listOf(
                            ClaimsQuery.mdoc(
                                id = ClaimId("age_over_18"),
                                namespace = namespace,
                                claimName = "age_over_18",
                            ),
                            ClaimsQuery.mdoc(
                                id = ClaimId("birth_date"),
                                namespace = namespace,
                                claimName = "birth_date",
                            ),
                        ),
                        claimSets = listOf(
                            ClaimSet(listOf(ClaimId("age_over_18"))),
                            ClaimSet(listOf(ClaimId("birth_date"))),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        // The wallet credential carries BOTH claims — both alternatives are individually
        // satisfiable. The processor still MUST pick only the first set per §6.4.1.
        val credentialClaims = listOf(
            mdocClaim(docType, namespace, "age_over_18", value = "true"),
            mdocClaim(docType, namespace, "birth_date", value = "1990-01-01"),
        )
        val processor = buildProcessor(
            credentialFormat = MsoMdocFormat(docType),
            credentialClaims = credentialClaims,
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        val match = success.assertSingleMatch()
        val disclosedDataElements = match.claims.keys
            .filterIsInstance<MdocRequestedClaim>()
            .map { it.dataElementName }
            .toSet()
        assertEquals(
            expected = setOf("age_over_18"),
            actual = disclosedDataElements,
            message = "First-satisfied claim_set must yield ONLY its claims. If `birth_date` " +
                "also surfaces, the processor is over-disclosing in violation of §6.4.1.",
        )
    }

    /**
     * Fall-through case: when the **first** claim_set cannot be satisfied (the credential
     * is missing one of its referenced claims) the processor must move on to the **next**
     * set and try it. Only the second set is satisfied here, so the match must carry
     * exactly that set's claims.
     *
     * This proves the loop in [DcqlRequestProcessor.resolveClaimsToDisclose] is wired
     * correctly (every set is tried; failure on one doesn't kill the credential outright)
     * **and** that the first set's already-resolved partial state doesn't leak into the
     * final match.
     */
    @Test
    fun `mdoc claim_sets first set unsatisfied, processor falls through to second`(): Unit = runBlocking {
        val docType = "org.iso.18013.5.1.mDL"
        val namespace = "org.iso.18013.5.1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.mdoc(
                        id = QueryId("query_0"),
                        msoMdocMeta = DCQLMetaMsoMdocExtensions(MsoMdocDocType(docType)),
                        claims = listOf(
                            ClaimsQuery.mdoc(
                                id = ClaimId("age_over_18"),
                                namespace = namespace,
                                claimName = "age_over_18",
                            ),
                            ClaimsQuery.mdoc(
                                id = ClaimId("birth_date"),
                                namespace = namespace,
                                claimName = "birth_date",
                            ),
                        ),
                        claimSets = listOf(
                            ClaimSet(listOf(ClaimId("age_over_18"))),
                            ClaimSet(listOf(ClaimId("birth_date"))),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        // The wallet only carries `birth_date` — the first claim_set fails, the second wins.
        val credentialClaims = listOf(
            mdocClaim(docType, namespace, "birth_date", value = "1990-01-01"),
        )
        val processor = buildProcessor(
            credentialFormat = MsoMdocFormat(docType),
            credentialClaims = credentialClaims,
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        val match = success.assertSingleMatch()
        val disclosedDataElements = match.claims.keys
            .filterIsInstance<MdocRequestedClaim>()
            .map { it.dataElementName }
            .toSet()
        assertEquals(
            expected = setOf("birth_date"),
            actual = disclosedDataElements,
            message = "When the first claim_set fails, the processor must fall through " +
                "and pick the next satisfied set — and ONLY that set's claims.",
        )
    }

    /**
     * `claim_sets` with **no** satisfied set must produce zero matches for the credential —
     * the processor returns `null` from [resolveClaimsToDisclose] and the candidate is
     * dropped (per §6.4.1 "If the Wallet cannot deliver all claims requested by the
     * Verifier according to these rules, it MUST NOT return the respective Credential").
     *
     */
    @Test
    fun `mdoc claim_sets no set satisfied produces no matches`(): Unit = runBlocking {
        val docType = "org.iso.18013.5.1.mDL"
        val namespace = "org.iso.18013.5.1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.mdoc(
                        id = QueryId("query_0"),
                        msoMdocMeta = DCQLMetaMsoMdocExtensions(MsoMdocDocType(docType)),
                        claims = listOf(
                            ClaimsQuery.mdoc(
                                id = ClaimId("age_over_18"),
                                namespace = namespace,
                                claimName = "age_over_18",
                            ),
                            ClaimsQuery.mdoc(
                                id = ClaimId("birth_date"),
                                namespace = namespace,
                                claimName = "birth_date",
                            ),
                        ),
                        claimSets = listOf(
                            ClaimSet(listOf(ClaimId("age_over_18"))),
                            ClaimSet(listOf(ClaimId("birth_date"))),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        // The wallet carries neither `age_over_18` nor `birth_date` — only `family_name`,
        // which isn't referenced by any claim_set. Both alternatives are unsatisfiable.
        val credentialClaims = listOf(
            mdocClaim(docType, namespace, "family_name", value = "Doe"),
        )
        val processor = buildProcessor(
            credentialFormat = MsoMdocFormat(docType),
            credentialClaims = credentialClaims,
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertTrue(
            success.presentmentData.credentialSets.isEmpty(),
            "A credential satisfying NO claim_set must not surface. " +
                "Found ${success.presentmentData.credentialSets.size} credential sets.",
        )
    }

    /**
     * Upstream-contract guard: [ClaimSet.ensureKnownClaimIds] rejects construction of a
     * DCQL whose claim_set references a claim id absent from the credential query's
     * `claims` list. The processor relies on this invariant — its
     * [resolveClaimsToDisclose] defensive branch (`claimIdLookup[claimId] == null` →
     * treat set as unsatisfied) is dead code under normal operation precisely because
     * the upstream construction throws first.
     *
     */
    @Test
    fun `DCQL construction rejects claim_set with unknown claim_id (upstream contract)`(): Unit = runBlocking {
        val docType = "org.iso.18013.5.1.mDL"
        val namespace = "org.iso.18013.5.1"

        val ex = kotlin.runCatching {
            DCQL(
                credentials = Credentials(
                    listOf(
                        CredentialQuery.mdoc(
                            id = QueryId("query_0"),
                            msoMdocMeta = DCQLMetaMsoMdocExtensions(MsoMdocDocType(docType)),
                            claims = listOf(
                                ClaimsQuery.mdoc(
                                    id = ClaimId("birth_date"),
                                    namespace = namespace,
                                    claimName = "birth_date",
                                ),
                            ),
                            claimSets = listOf(
                                // References an id that is NOT declared in `claims` above.
                                // [ClaimSet.ensureKnownClaimIds] MUST throw on construction.
                                ClaimSet(listOf(ClaimId("unknown_claim_id"))),
                                ClaimSet(listOf(ClaimId("birth_date"))),
                            ),
                        ),
                    ),
                ),
                credentialSets = null,
            )
        }.exceptionOrNull()

        assertIs<IllegalArgumentException>(
            value = ex,
            message = "DCQL construction must reject a claim_set referencing an unknown " +
                "claim_id. If this assertion ever stops holding, the wallet's " +
                "[DcqlRequestProcessor.resolveClaimsToDisclose] defensive branch becomes " +
                "live code and needs its own integration coverage.",
        )
        assertEquals(
            ex.message?.contains("Unknown claim ids"),
            true,
            "Expected the upstream error to mention 'Unknown claim ids'; got: ${ex.message}"
        )
    }

    /**
     * Format symmetry: `claim_sets` first-match semantics must be identical for
     * `dc+sd-jwt` credentials. The same algorithm (`resolveClaimsToDisclose`) drives both
     * format paths, so a single sdjwt test is a cheap guard against accidental format-
     * specific drift.
     *
     * Setup: an SD-JWT VC carrying `given_name` and `family_name`. The verifier lists them
     * as alternatives. The processor must pick the first.
     */
    @Test
    fun `sdjwt claim_sets first satisfied set wins, no over-disclosure`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        claims = listOf(
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("given_name"),
                                path = ClaimPath(listOf(ClaimPathElement.Claim("given_name"))),
                            ),
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("family_name"),
                                path = ClaimPath(listOf(ClaimPathElement.Claim("family_name"))),
                            ),
                        ),
                        claimSets = listOf(
                            ClaimSet(listOf(ClaimId("given_name"))),
                            ClaimSet(listOf(ClaimId("family_name"))),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf<Claim>(
            jsonClaim(vct, claimName = "given_name", value = JsonPrimitive("Alice")),
            jsonClaim(vct, claimName = "family_name", value = JsonPrimitive("Doe")),
        )
        val processor = buildSdJwtProcessor(vct = vct, credentialClaims = credentialClaims)

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        val match = success.assertSingleMatch()
        val disclosedPaths: Set<JsonArray> = match.claims.keys
            .filterIsInstance<JsonRequestedClaim>()
            .map { it.claimPath }
            .toSet()
        assertEquals(
            expected = setOf(JsonArray(listOf(JsonPrimitive("given_name")))),
            actual = disclosedPaths,
            message = "SD-JWT first-satisfied claim_set must yield ONLY its claim. If " +
                "`family_name` also surfaces, the SD-JWT path is over-disclosing.",
        )
    }

    /**
     * §6.3 `values` filter at the top level: verifier asks for `family_name`
     * restricted to `"Smith"`. A credential whose `family_name` is `"Smith"`
     * must surface as a match.
     */
    @Test
    fun `sdjwt top-level claim with values filter matches when value is in list`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        claims = listOf(
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("family_name"),
                                path = ClaimPath(listOf(ClaimPathElement.Claim("family_name"))),
                                values = JsonArray(listOf(JsonPrimitive("Smith"))),
                            ),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf<Claim>(
            jsonClaim(vct, claimName = "family_name", value = JsonPrimitive("Smith")),
        )
        val processor = buildSdJwtProcessor(vct = vct, credentialClaims = credentialClaims)

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        success.assertSingleMatch()
    }

    /**
     * §6.3 `values` filter negative case: same query as above, but the credential
     * carries `family_name = "Brown"`. The wallet must NOT surface this
     * credential — and per §6.4.2 "every credentials entry is required" the whole
     * request becomes unsatisfiable, so the resulting presentment tree is empty.
     */
    @Test
    fun `sdjwt top-level claim with values filter rejects when value is not in list`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        claims = listOf(
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("family_name"),
                                path = ClaimPath(listOf(ClaimPathElement.Claim("family_name"))),
                                values = JsonArray(listOf(JsonPrimitive("Smith"))),
                            ),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf<Claim>(
            jsonClaim(vct, claimName = "family_name", value = JsonPrimitive("Brown")),
        )
        val processor = buildSdJwtProcessor(vct = vct, credentialClaims = credentialClaims)

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertNoMatches(success)
    }

    /**
     * Nested path + `values`: verifier asks for `place_of_birth.country` restricted
     * to `"Greece"`. The credential's nested `country` claim equals `"Greece"` and
     * must produce a match.
     */
    @Test
    fun `sdjwt nested claim with values filter matches when nested value is in list`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        claims = listOf(
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("country"),
                                path = ClaimPath(
                                    listOf(
                                        ClaimPathElement.Claim("place_of_birth"),
                                        ClaimPathElement.Claim("country"),
                                    ),
                                ),
                                values = JsonArray(listOf(JsonPrimitive("Greece"))),
                            ),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf<Claim>(
            jsonClaim(
                vct = vct,
                claimName = "place_of_birth",
                value = buildJsonObject {
                    put("country", JsonPrimitive("Greece"))
                    put("city", JsonPrimitive("Athens"))
                },
            ),
        )
        val processor = buildSdJwtProcessor(vct = vct, credentialClaims = credentialClaims)

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        success.assertSingleMatch()
    }

    /**
     * Nested path + `values` negative case: same query, but the credential's
     * `place_of_birth.country` is `"France"`. The processor must NOT surface this
     * credential — `values` filtering must apply end-to-end, not just at the top
     * level.
     */
    @Test
    fun `sdjwt nested claim with values filter rejects when nested value is not in list`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        claims = listOf(
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("country"),
                                path = ClaimPath(
                                    listOf(
                                        ClaimPathElement.Claim("place_of_birth"),
                                        ClaimPathElement.Claim("country"),
                                    ),
                                ),
                                values = JsonArray(listOf(JsonPrimitive("Greece"))),
                            ),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf<Claim>(
            jsonClaim(
                vct = vct,
                claimName = "place_of_birth",
                value = buildJsonObject {
                    put("country", JsonPrimitive("France"))
                    put("city", JsonPrimitive("Paris"))
                },
            ),
        )
        val processor = buildSdJwtProcessor(vct = vct, credentialClaims = credentialClaims)

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertNoMatches(success)
    }

    /**
     * Non-trailing wildcard + `values`: verifier asks for `addresses[*].city`
     * restricted to `"Athens"`. The credential's `addresses` array contains an
     * element whose `city` equals `"Athens"`, so per §6.4.1 + §7.1 element-by-
     * element semantics the credential must match.
     *
     * Handled by the wallet's `matchClaimViaSpecCorrectNullWildcard` fallback,
     * which covers the wildcard + values combination.
     */
    @Test
    fun `sdjwt non-trailing wildcard with values filter matches when any element matches`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        claims = listOf(
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("city_anywhere"),
                                path = ClaimPath(
                                    listOf(
                                        ClaimPathElement.Claim("addresses"),
                                        ClaimPathElement.AllArrayElements,
                                        ClaimPathElement.Claim("city"),
                                    ),
                                ),
                                values = JsonArray(listOf(JsonPrimitive("Athens"))),
                            ),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf<Claim>(
            jsonClaim(
                vct = vct,
                claimName = "addresses",
                value = buildJsonArray {
                    add(buildJsonObject { put("city", JsonPrimitive("Berlin")) })
                    add(buildJsonObject { put("city", JsonPrimitive("Athens")) })
                },
            ),
        )
        val processor = buildSdJwtProcessor(vct = vct, credentialClaims = credentialClaims)

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        success.assertSingleMatch()
    }

    /**
     * Non-trailing wildcard + `values` negative case: none of the `addresses[*].city`
     * entries equals `"Athens"`, so the credential must not surface as a match.
     */
    @Test
    fun `sdjwt non-trailing wildcard with values filter rejects when no element matches`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        claims = listOf(
                            ClaimsQuery.sdJwtVc(
                                id = ClaimId("city_anywhere"),
                                path = ClaimPath(
                                    listOf(
                                        ClaimPathElement.Claim("addresses"),
                                        ClaimPathElement.AllArrayElements,
                                        ClaimPathElement.Claim("city"),
                                    ),
                                ),
                                values = JsonArray(listOf(JsonPrimitive("Athens"))),
                            ),
                        ),
                    ),
                ),
            ),
            credentialSets = null,
        )

        val credentialClaims = listOf<Claim>(
            jsonClaim(
                vct = vct,
                claimName = "addresses",
                value = buildJsonArray {
                    add(buildJsonObject { put("city", JsonPrimitive("Berlin")) })
                    add(buildJsonObject { put("city", JsonPrimitive("Paris")) })
                },
            ),
        )
        val processor = buildSdJwtProcessor(vct = vct, credentialClaims = credentialClaims)

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertNoMatches(success)
    }

    /**
     * Asserts that [processed]'s presentment tree contains no matches at all.
     * Used by the values-filter negative-case tests: when the only credential the
     * wallet holds fails the filter, §6.4.2 makes the whole request unsatisfiable
     * and the matcher must return an empty set of credential sets.
     */
    private fun assertNoMatches(processed: ProcessedDcqlRequest) {
        val matches = processed.presentmentData
            .credentialSets.flatMap { it.options }
            .flatMap { it.members }
            .flatMap { it.matches }
        assertEquals(
            expected = 0,
            actual = matches.size,
            message = "Expected zero matches; got $matches",
        )
    }

    /**
     * Build a real [MdocClaim] for the given namespace + dataElement, backed by a CBOR
     * [Tstr] value. The actual value is irrelevant to the matcher; only the
     * (namespace, dataElement) tuple matters for [findMatchingClaim].
     */
    private fun mdocClaim(
        docType: String,
        namespace: String,
        dataElementName: String,
        value: String,
    ): MdocClaim = MdocClaim(
        displayName = dataElementName,
        attribute = null,
        docType = docType,
        namespaceName = namespace,
        dataElementName = dataElementName,
        value = Tstr(value),
    )

    /**
     * Assemble a [DcqlRequestProcessor] backed by a single mocked [IssuedDocument] whose
     * credential exposes [credentialClaims]. The trust source is wired to a relaxed
     * `OpenId4VpReaderTrust` returning [ReaderTrustResult.Pending] — sufficient for the
     * matching code path, which doesn't depend on trust verdict.
     */
    private fun buildProcessor(
        credentialFormat: MsoMdocFormat,
        credentialClaims: List<MdocClaim>,
    ): DcqlRequestProcessor {
        val credential = mockk<SecureAreaBoundCredential> {
            coEvery { getClaims(documentTypeRepository = null) } returns credentialClaims
        }
        val issuedDoc = mockk<IssuedDocument> {
            every { format } returns credentialFormat
            coEvery { findCredential(now = any()) } returns credential
        }
        val documentManager = mockk<DocumentManager> {
            every { getDocuments(predicate = any()) } returns listOf(issuedDoc)
            every { getDocuments(predicate = null) } returns listOf(issuedDoc)
        }
        val trust = mockk<OpenId4VpReaderTrust> {
            every { result } returns ReaderTrustResult.Pending
            every { readerTrustStore } returns null
            every { readerTrustStore = any() } returns Unit
        }
        return DcqlRequestProcessor(documentManager, trust)
    }

    /**
     * Build an [OpenId4VpRequest] wrapping the given [dcql]. The wrapper has no transaction
     * data and uses a [Client.RedirectUri] — chosen because its `legalName()` is `null`
     * (no certificate to parse) which keeps the trust path inert.
     */
    private fun buildOpenId4VpRequest(dcql: DCQL): OpenId4VpRequest {
        val resolved = mockk<ResolvedRequestObject> {
            every { query } returns dcql
            every { transactionData } returns null
            every { client } returns Client.RedirectUri(URI.create("https://verifier.example"))
        }
        return mockk { every { resolvedRequestObject } returns resolved }
    }

    /**
     * Convenience walker over a [ProcessedDcqlRequest]'s presentment tree that asserts
     * exactly one match is surfaced and returns it. Several claim_sets tests assert the
     * shape of `match.claims` and would otherwise repeat the same flatten boilerplate.
     */
    private fun ProcessedDcqlRequest.assertSingleMatch(): CredentialPresentmentSetOptionMemberMatch {
        val matches = presentmentData
            .credentialSets.flatMap { it.options }
            .flatMap { it.members }
            .flatMap { it.matches }
        assertEquals(
            expected = 1,
            actual = matches.size,
            message = "Expected exactly one match in the presentment tree; got ${matches.size}",
        )
        return matches.single()
    }

    /**
     * Build a real [JsonClaim] for an SD-JWT VC credential. The [claimPath] is a
     * single-element [JsonArray] of the claim name — enough for the path-prefix matching
     * that [findMatchingClaim] performs against a request's [JsonRequestedClaim.claimPath].
     * Anything deeper than top-level claims would also exercise SD-JWT array-index /
     * wildcard handling, which is out of scope for the claim_sets first-match tests.
     */
    private fun jsonClaim(
        vct: String,
        claimName: String,
        value: JsonElement,
    ): JsonClaim = JsonClaim(
        displayName = claimName,
        attribute = null,
        vct = vct,
        claimPath = JsonArray(listOf(JsonPrimitive(claimName))),
        value = value,
    )

    /**
     * SD-JWT analogue of [buildProcessor]. The mocked [IssuedDocument] reports
     * [SdJwtVcFormat] for the given `vct`, and its credential exposes the passed
     * [credentialClaims] (typed [Claim] so callers can mix [JsonClaim] subtypes).
     */
    private fun buildSdJwtProcessor(
        vct: String,
        credentialClaims: List<Claim>,
    ): DcqlRequestProcessor {
        val credential = mockk<SecureAreaBoundCredential> {
            coEvery { getClaims(documentTypeRepository = null) } returns credentialClaims
        }
        val issuedDoc = mockk<IssuedDocument> {
            every { format } returns SdJwtVcFormat(vct) as DocumentFormat
            coEvery { findCredential(now = any()) } returns credential
        }
        val documentManager = mockk<DocumentManager> {
            every { getDocuments(predicate = any()) } returns listOf(issuedDoc)
            every { getDocuments(predicate = null) } returns listOf(issuedDoc)
        }
        val trust = mockk<OpenId4VpReaderTrust> {
            every { result } returns ReaderTrustResult.Pending
            every { readerTrustStore } returns null
            every { readerTrustStore = any() } returns Unit
        }
        return DcqlRequestProcessor(documentManager, trust)
    }
}