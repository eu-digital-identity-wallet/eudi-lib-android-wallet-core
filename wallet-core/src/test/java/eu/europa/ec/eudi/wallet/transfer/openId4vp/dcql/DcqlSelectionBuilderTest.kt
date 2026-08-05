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

import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import io.mockk.every
import io.mockk.mockk
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [buildMultipleAwareSelections]. Covers how the per-query `multiple` flag
 * shapes the list of options, the default when the flag is missing, mixed flags across
 * queries, and how optional and multi-option sets are handled.
 */
class DcqlSelectionBuilderTest {

    /** With `multiple = false`, two candidates produce two options — one per credential. */
    @Test
    fun `multiple=false with 2 matches expands into 2 single-match selections`() {
        val mdlA = matchWithQueryId(QueryId("mdl"))
        val mdlB = matchWithQueryId(QueryId("mdl"))
        val data = oneSetOneOption(matches = listOf(mdlA, mdlB))

        val selections = buildMultipleAwareSelections(
            data = data,
            multipleByQueryId = mapOf(QueryId("mdl") to false),
        )

        assertEquals(2, selections.size, "multiple=false should fan out per match")
        assertEquals(listOf(listOf(mdlA), listOf(mdlB)), selections.map { it.matches })
    }

    /** With `multiple = true`, two candidates are grouped into one option containing both. */
    @Test
    fun `multiple=true with 2 matches collapses into a single selection`() {
        val mdlA = matchWithQueryId(QueryId("mdl"))
        val mdlB = matchWithQueryId(QueryId("mdl"))
        val data = oneSetOneOption(matches = listOf(mdlA, mdlB))

        val selections = buildMultipleAwareSelections(
            data = data,
            multipleByQueryId = mapOf(QueryId("mdl") to true),
        )

        assertEquals(1, selections.size, "multiple=true should bundle matches into one selection")
        assertEquals(listOf(mdlA, mdlB), selections.single().matches)
    }

    /** A missing entry in `multipleByQueryId` is treated as `false`. */
    @Test
    fun `missing multiple flag defaults to false (fan-out)`() {
        val mdlA = matchWithQueryId(QueryId("mdl"))
        val mdlB = matchWithQueryId(QueryId("mdl"))
        val data = oneSetOneOption(matches = listOf(mdlA, mdlB))

        val selections = buildMultipleAwareSelections(
            data = data,
            multipleByQueryId = mapOf("other-query" to true).mapKeys { (k, _) -> QueryId(k) },
        )

        assertEquals(2, selections.size)
        assertEquals(listOf(listOf(mdlA), listOf(mdlB)), selections.map { it.matches })
    }

    /**
     * Two queries with different flags. The `multiple = false` query gives one option per
     * candidate; the `multiple = true` query groups its candidates into one. The final list
     * pairs every single-candidate option of the first query with the grouped option of
     * the second.
     */
    @Test
    fun `mixed multiple flags combine fan-out and bundle`() {
        val pidA = matchWithQueryId(QueryId("pid"))
        val pidB = matchWithQueryId(QueryId("pid"))
        val mdlA = matchWithQueryId(QueryId("mdl"))
        val mdlB = matchWithQueryId(QueryId("mdl"))
        val data = CredentialPresentmentData(
            credentialSets = listOf(
                singleOptionSet(optional = false, matches = listOf(pidA, pidB)),
                singleOptionSet(optional = false, matches = listOf(mdlA, mdlB)),
            ),
        )

        val selections = buildMultipleAwareSelections(
            data = data,
            multipleByQueryId = mapOf(
                QueryId("pid") to false,
                QueryId("mdl") to true,
            ),
        )

        assertEquals(2, selections.size, "expected 2 variants: one per pid × one bundled mdl pair")
        val flattened = selections.map { it.matches }
        assertTrue(listOf(pidA, mdlA, mdlB) in flattened)
        assertTrue(listOf(pidB, mdlA, mdlB) in flattened)
    }

    /** `multiple = true` with a single candidate produces one option containing that match. */
    @Test
    fun `multiple=true with 1 match yields one selection of one match`() {
        val mdlA = matchWithQueryId(QueryId("mdl"))
        val data = oneSetOneOption(matches = listOf(mdlA))

        val selections = buildMultipleAwareSelections(
            data = data,
            multipleByQueryId = mapOf(QueryId("mdl") to true),
        )

        assertEquals(1, selections.size)
        assertEquals(listOf(mdlA), selections.single().matches)
    }

    /** An optional set with `multiple = true` produces two options: skip the set, or include all its matches. */
    @Test
    fun `optional set with multiple=true emits both skip and bundle variants`() {
        val loyaltyA = matchWithQueryId(QueryId("loyalty"))
        val loyaltyB = matchWithQueryId(QueryId("loyalty"))
        val data = CredentialPresentmentData(
            credentialSets = listOf(
                singleOptionSet(optional = true, matches = listOf(loyaltyA, loyaltyB)),
            ),
        )

        val selections = buildMultipleAwareSelections(
            data = data,
            multipleByQueryId = mapOf(QueryId("loyalty") to true),
        )

        assertEquals(2, selections.size)
        assertTrue(selections.any { it.matches.isEmpty() }, "expected a skip variant")
        assertTrue(
            selections.any { it.matches == listOf(loyaltyA, loyaltyB) },
            "expected a variant bundling both loyalty matches",
        )
    }

    /**
     * In a set with two options, each option produces its own entry in the output. The
     * `multiple` flag is still applied within each option to the matches of the query it
     * refers to.
     */
    @Test
    fun `set with multiple options applies multiple flag inside each option`() {
        val optionAMdlA = matchWithQueryId(QueryId("mdl"))
        val optionAMdlB = matchWithQueryId(QueryId("mdl"))
        val optionBPidA = matchWithQueryId(QueryId("pid"))
        val data = CredentialPresentmentData(
            credentialSets = listOf(
                CredentialPresentmentSet(
                    optional = false,
                    options = listOf(
                        CredentialPresentmentSetOption(
                            members = listOf(
                                CredentialPresentmentSetOptionMember(matches = listOf(optionAMdlA, optionAMdlB)),
                            ),
                        ),
                        CredentialPresentmentSetOption(
                            members = listOf(
                                CredentialPresentmentSetOptionMember(matches = listOf(optionBPidA)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val selections = buildMultipleAwareSelections(
            data = data,
            multipleByQueryId = mapOf(
                QueryId("mdl") to true,
                QueryId("pid") to false,
            ),
        )

        assertEquals(2, selections.size, "expected one variant per option")
        val flattened = selections.map { it.matches }
        assertTrue(listOf(optionAMdlA, optionAMdlB) in flattened, "mdl option bundles both matches")
        assertTrue(listOf(optionBPidA) in flattened, "pid option keeps single match")
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Build a mock match whose source reports the given [QueryId]. That is the only field
     * the function under test reads from the match, so the rest is left as a plain mock.
     */
    private fun matchWithQueryId(queryId: QueryId): CredentialPresentmentSetOptionMemberMatch {
        val credentialQuery = mockk<DcqlCredentialQuery> {
            every { id } returns queryId.value
        }
        return mockk<CredentialPresentmentSetOptionMemberMatch> {
            every { source } returns CredentialMatchSourceOpenID4VP(credentialQuery = credentialQuery)
        }
    }

    /** Builds the default DCQL layout: one set with one option that has one member. */
    private fun oneSetOneOption(
        matches: List<CredentialPresentmentSetOptionMemberMatch>,
        optional: Boolean = false,
    ): CredentialPresentmentData = CredentialPresentmentData(
        credentialSets = listOf(singleOptionSet(optional = optional, matches = matches)),
    )

    /** Builds a [CredentialPresentmentSet] that has one option containing one member. */
    private fun singleOptionSet(
        optional: Boolean,
        matches: List<CredentialPresentmentSetOptionMemberMatch>,
    ): CredentialPresentmentSet = CredentialPresentmentSet(
        optional = optional,
        options = listOf(
            CredentialPresentmentSetOption(
                members = listOf(CredentialPresentmentSetOptionMember(matches = matches)),
            ),
        ),
    )
}