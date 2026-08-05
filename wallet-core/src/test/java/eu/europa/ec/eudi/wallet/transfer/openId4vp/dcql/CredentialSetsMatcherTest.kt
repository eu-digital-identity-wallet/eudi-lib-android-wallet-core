package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQueryIds
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSetQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSets
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import org.junit.Before
import org.junit.Test
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [CredentialSetsMatcher.toCredentialPresentmentSets], which arranges
 * per-query matches into a presentment tree according to OpenID4VP §6.4.2
 * `credential_sets` rules.
 *
 * Test inputs are kept symbolic: each "available" query is represented by a named
 * [CredentialPresentmentSetOptionMemberMatch] sentinel so we can assert the
 * wallet-resolved match flows through to the correct leaf in the returned tree,
 * without depending on real credential storage.
 */
class CredentialSetsMatcherTest {

    private lateinit var matcher: CredentialSetsMatcher

    @Before
    fun setup() {
        matcher = CredentialSetsMatcher()
    }

    /**
     * Dummy [CredentialQuery] — the matcher only reads its id; format and meta are
     * irrelevant and can be mocked away.
     */
    private fun query(id: String): CredentialQuery = CredentialQuery(
        id = QueryId(id),
        format = mockk<Format>(relaxed = true),
        meta = mockk<JsonObject>(relaxed = true),
    )

    /**
     * A sentinel match instance tagged by its source query id; used to verify the matcher routes
     * inputs to the correct presentment leaf.
     */
    private fun sentinelMatch(forQuery: String): CredentialPresentmentSetOptionMemberMatch =
        mockk(name = "match-$forQuery")

    /** Flatten all matches anywhere in the returned tree. */
    private fun List<CredentialPresentmentSet>.allMatches(): List<CredentialPresentmentSetOptionMemberMatch> =
        flatMap { set -> set.options.flatMap { opt -> opt.members.flatMap { m -> m.matches } } }

    /**
     * Absent credential_sets + wallet has matches for every query in `credentials` ⇒ each query
     * becomes its own non-optional [CredentialPresentmentSet] carrying its matches.
     */
    @Test
    fun `credentialSets is null and wallet has all documents, should produce one set per query`() {
        val match0 = sentinelMatch("query_0")
        val match1 = sentinelMatch("query_1")
        val credentials = Credentials(listOf(query("query_0"), query("query_1")))

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = null,
            matchesByQueryId = mapOf(
                QueryId("query_0") to listOf(match0),
                QueryId("query_1") to listOf(match1),
            ),
        )

        assertEquals(2, result.size)
        val matches = result.allMatches()
        assertTrue(matches.contains(match0))
        assertTrue(matches.contains(match1))
        assertTrue(result.none { it.optional }, "Implicit-required sets must be non-optional")
    }

    /**
     * Absent credential_sets + wallet is missing at least one required query ⇒ unsatisfiable;
     * the matcher returns an empty list to signal "request cannot be served".
     */
    @Test
    fun `credentialSets is null and wallet is missing a document, should return empty list`() {
        val credentials = Credentials(listOf(query("query_0"), query("query_1")))

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = null,
            matchesByQueryId = mapOf(
                // query_1 is missing → request unsatisfiable
                QueryId("query_0") to listOf(sentinelMatch("query_0")),
            ),
        )

        assertTrue(result.isEmpty(), "Missing required document must yield an empty list")
    }

    /**
     * One required set referencing `query_0` + wallet has a match for `query_0` ⇒ the matcher
     * produces one non-optional set with that match exposed.
     */
    @Test
    fun `single required set is satisfied, should expose its match`() {
        val match0 = sentinelMatch("query_0")
        val credentials = Credentials(listOf(query("query_0"), query("query_1")))
        val credentialSets = CredentialSets(
            listOf(
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("query_0")))),
                    required = true,
                ),
            ),
        )

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = credentialSets,
            matchesByQueryId = mapOf(QueryId("query_0") to listOf(match0)),
        )

        assertEquals(1, result.size)
        assertTrue(!result.single().optional)
        assertTrue(result.allMatches().contains(match0))
    }

    /**
     * One required set referencing `query_0` but wallet has no match for it ⇒ the required set
     * is unsatisfiable; matcher returns empty list.
     */
    @Test
    fun `single required set is not satisfied, should return empty list`() {
        val credentials = Credentials(listOf(query("query_0"), query("query_1")))
        val credentialSets = CredentialSets(
            listOf(
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("query_0")))),
                    required = true,
                ),
            ),
        )

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = credentialSets,
            // Wallet has the wrong query; the required set cannot be satisfied.
            matchesByQueryId = mapOf(QueryId("query_1") to listOf(sentinelMatch("query_1"))),
        )

        assertTrue(result.isEmpty(), "Required set unsatisfied ⇒ empty list")
    }

    /**
     * Required set with multiple options where a shared dependency (`query_0`) is missing ⇒ no
     * option can be satisfied, returns empty list.
     */
    @Test
    fun `none of the satisfying options in a required credential_set is met, returns empty list`() {
        val credentials = Credentials(listOf(query("query_0"), query("query_1"), query("query_2")))
        val credentialSets = CredentialSets(
            listOf(
                CredentialSetQuery(
                    options = listOf(
                        CredentialQueryIds(listOf(QueryId("query_0"), QueryId("query_2"))),
                        CredentialQueryIds(listOf(QueryId("query_0"), QueryId("query_1"))),
                    ),
                ),
            ),
        )

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = credentialSets,
            matchesByQueryId = mapOf(
                // query_0 is the shared dependency for every option — and it's missing.
                QueryId("query_1") to listOf(sentinelMatch("query_1")),
                QueryId("query_2") to listOf(sentinelMatch("query_2")),
            ),
        )

        assertTrue(result.isEmpty())
    }

    /**
     * One required set (satisfied) and one optional set (not satisfied) ⇒ only the required set
     * is emitted; the optional one is silently dropped.
     */
    @Test
    fun `required set satisfied but optional is not, should expose only the required match`() {
        val matchRequired = sentinelMatch("query_0")
        val credentials = Credentials(listOf(query("query_0"), query("query_1")))
        val credentialSets = CredentialSets(
            listOf(
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("query_0")))),
                ),
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("query_1")))),
                    required = false,
                ),
            ),
        )

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = credentialSets,
            matchesByQueryId = mapOf(QueryId("query_0") to listOf(matchRequired)),
        )

        assertEquals(1, result.size)
        assertEquals(false, result.single().optional, "Required set must remain non-optional")
        assertTrue(result.allMatches().contains(matchRequired))
    }

    /**
     * Required set with two options; the wallet cannot satisfy the first (preferred) option but
     * can satisfy the second ⇒ the second option is emitted with its constituent matches.
     */
    @Test
    fun `required set with multiple options is satisfied by second option, should expose its matches`() {
        val match0 = sentinelMatch("query_0")
        val match1 = sentinelMatch("query_1")
        val credentials = Credentials(listOf(query("query_0"), query("query_1"), query("query_2")))
        val credentialSets = CredentialSets(
            listOf(
                CredentialSetQuery(
                    options = listOf(
                        CredentialQueryIds(
                            listOf(
                                QueryId("query_0"),
                                QueryId("query_2")
                            )
                        ), // preferred, not satisfiable
                        CredentialQueryIds(
                            listOf(
                                QueryId("query_0"),
                                QueryId("query_1")
                            )
                        ), // satisfiable
                    )
                )
            ),
        )

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = credentialSets,
            matchesByQueryId = mapOf(
                QueryId("query_0") to listOf(match0),
                QueryId("query_1") to listOf(match1),
                // query_2 deliberately missing
            )
        )

        assertEquals(1, result.size)
        val matches = result.allMatches()
        assertTrue(matches.contains(match0))
        assertTrue(matches.contains(match1))
    }

    /**
     * Top-level `credentials` may carry queries not referenced by `credential_sets`; those are
     * ignored. If the sole set referenced is optional and unsatisfied, no sets are emitted.
     */
    @Test
    fun `credential not in credential_sets is ignored, and unsatisfied optional set yields empty list`() {
        val credentials = Credentials(listOf(query("query_0"), query("query_1")))
        val credentialSets = CredentialSets(
            listOf(
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("query_1")))),
                    required = false
                )
            )
        )

        val result = matcher.toCredentialPresentmentSets(
            credentials = credentials,
            credentialSets = credentialSets,
            matchesByQueryId = mapOf(
                // query_0 is matched but it isn't whitelisted by credential_sets, so it's ignored;
                // query_1 is whitelisted but missing, so the optional set is dropped.
                QueryId("query_0") to listOf(sentinelMatch("query_0"))
            )
        )

        assertTrue(result.isEmpty(), "Whitelisted query missing + optional set ⇒ empty list")
    }
}