package eu.europa.ec.eudi.wallet.transfer.dcql

import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.CredentialSetsMatcher
import kotlin.test.*
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSetQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSets
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQueryIds

class CredentialSetsMatcherTest {

    private lateinit var matcher: CredentialSetsMatcher

    // Helper function to easily create dummy CredentialQuery objects for testing.
    // The matcher logic only cares about the 'id', so format and meta can be mocked.
    private fun createCredentialQuery(id: String): CredentialQuery {
        return CredentialQuery(
            id = QueryId(id),
            format = mockk<Format>(relaxed = true),
            meta = mockk<JsonObject>(relaxed = true)
        )
    }

    @Before
    fun setup() {
        matcher = CredentialSetsMatcher()
    }

    /**
     * Test based on absent_credential_sets_pid_mdl.json
     * Scenario: The request does not contain a 'credential_sets' array.
     * Wallet State: The wallet has both documents requested ('query_0', 'query_1').
     * Expected Result: The matcher should return both credential queries, as all are
     * implicitly required and available.
     */
    @Test
    fun `credentialSets is null and wallet has all documents, should return all credentials`() {

        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val credentials = Credentials(listOf(query0, query1))
        val availableWalletIds = setOf(QueryId("query_0"), QueryId("query_1"))

        val result = matcher.determineRequestedDocuments(
            credentials = credentials,
            credentialSets = null, // credential_sets is absent
            availableWalletCredentialIds = availableWalletIds
        )

        assertEquals(2, result.size)
        assertTrue(result.containsKey(QueryId("query_0")))
        assertTrue(result.containsKey(QueryId("query_1")))
    }

    /**
     * Test based on absent_credential_sets_pid_mdl.json
     * Scenario: The request does not contain a 'credential_sets' array.
     * Wallet State: The wallet is missing one of the two required documents.
     * Expected Result: The matcher must return an empty map, as the "all-or-nothing"
     * rule is not satisfied.
     */
    @Test
    fun `credentialSets is null and wallet is missing a document, should return empty map`() {

        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val credentials = Credentials(listOf(query0, query1))
        // The wallet only has the document for query_0, but is missing query_1
        val availableWalletIds = setOf(QueryId("query_0"))

        val result = matcher.determineRequestedDocuments(
            credentials = credentials,
            credentialSets = null,
            availableWalletCredentialIds = availableWalletIds
        )

        assertTrue(result.isEmpty(), "Expected result to be an empty map but it was not.")
    }

    /**
     * Test based on credential_sets_singe_required.json
     * Scenario: The request has one required credential set asking for 'query_0'.
     * Wallet State: The wallet has the document for 'query_0'.
     * Expected Result: The matcher should return only the credential for 'query_0'.
     */
    @Test
    fun `single required set is satisfied, should return credentials from that set`() {

        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val credentials = Credentials(listOf(query0, query1))

        val requiredSet = CredentialSetQuery(
            options = listOf(CredentialQueryIds(listOf(QueryId("query_0")))),
            required = true
        )
        val credentialSets = CredentialSets(listOf(requiredSet))

        val availableWalletIds = setOf(QueryId("query_0"))

        val result = matcher.determineRequestedDocuments(
            credentials = credentials,
            credentialSets = credentialSets,
            availableWalletCredentialIds = availableWalletIds
        )

        assertEquals(1, result.size)
        assertTrue(result.containsKey(QueryId("query_0")))
    }

    /**
     * Test based on credential_sets_singe_required.json
     * Scenario: The request has one required credential set asking for 'query_0'.
     * Wallet State: The wallet does NOT have the document for 'query_0'.
     * Expected Result: The matcher must return an empty map because the required
     * set cannot be satisfied.
     */
    @Test
    fun `single required set is not satisfied, should return empty map`() {

        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val credentials = Credentials(listOf(query0, query1))

        val requiredSet = CredentialSetQuery(
            options = listOf(CredentialQueryIds(listOf(QueryId("query_0")))),
            required = true
        )
        val credentialSets = CredentialSets(listOf(requiredSet))

        // The wallet has a different document, but not the required one.
        val availableWalletIds = setOf(QueryId("query_1"))

        val result = matcher.determineRequestedDocuments(
            credentials = credentials,
            credentialSets = credentialSets,
            availableWalletCredentialIds = availableWalletIds
        )

        assertTrue(result.isEmpty(), "Expected an empty map because the required set was not satisfied.")
    }

    /**
     * Test based on credentials_sets_multiple_required.json
     * Scenario: A required set has multiple options, but none can be met because a
     * common credential ('query_0') is missing.
     * The wallet is missing 'query_0'.
     * Expected Result: The matcher must return an empty map.
     */
    @Test
    fun `none in a list of satisfying options in a 'required' credential_set is met`() {
        // Arrange
        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val query2 = createCredentialQuery("query_2")
        val credentials = Credentials(listOf(query0, query1, query2))

        val requiredSet = CredentialSetQuery(
            options = listOf(
                CredentialQueryIds(listOf(QueryId("query_0"), QueryId("query_2"))),
                CredentialQueryIds(listOf(QueryId("query_0"), QueryId("query_1")))
            )
        )
        val credentialSets = CredentialSets(listOf(requiredSet))

        // Wallet is missing the crucial query_0, so neither option can be satisfied.
        val availableWalletIds = setOf(QueryId("query_1"), QueryId("query_2"))

        val result = matcher.determineRequestedDocuments(credentials, credentialSets, availableWalletIds)

        assertTrue(result.isEmpty())
    }

    /**
     * Test based on credentials_sets_required_optional.json
     * Scenario: The request has a required set and an optional set.
     * Wallet State: The wallet satisfies the required set but not the optional one.
     * Expected Result: The matcher should return only the credentials from the required set.
     */
    @Test
    fun `required set satisfied but optional is not, should return only required credentials`() {

        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val credentials = Credentials(listOf(query0, query1))

        val requiredSet = CredentialSetQuery(options = listOf(CredentialQueryIds(listOf(QueryId("query_0")))))
        val optionalSet = CredentialSetQuery(
            options = listOf(CredentialQueryIds(listOf(QueryId("query_1")))),
            required = false
        )
        val credentialSets = CredentialSets(listOf(requiredSet, optionalSet))

        val availableWalletIds = setOf(QueryId("query_0")) // Missing query_1 for the optional set

        val result = matcher.determineRequestedDocuments(credentials, credentialSets, availableWalletIds)

        assertEquals(1, result.size)
        assertTrue(result.containsKey(QueryId("query_0")))
    }

    /**
     * Test based on credentials_sets_mutiple_required.json
     * Scenario: A required set has two options. The wallet cannot satisfy the first (preferred)
     * option but can satisfy the second.
     * Wallet State: Has query_0 and query_1, but is missing query_2.
     * Expected Result: The matcher should select the second option and return query_0 and query_1.
     */
    @Test
    fun `required set with multiple options is satisfied by second option, should return credentials from second option`() {

        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val query2 = createCredentialQuery("query_2")
        val credentials = Credentials(listOf(query0, query1, query2))

        val requiredSet = CredentialSetQuery(
            options = listOf(
                CredentialQueryIds(listOf(QueryId("query_0"), QueryId("query_2"))), // Preferred, but not possible
                CredentialQueryIds(listOf(QueryId("query_0"), QueryId("query_1")))  // Possible
            )
        )
        val credentialSets = CredentialSets(listOf(requiredSet))

        val availableWalletIds = setOf(QueryId("query_0"), QueryId("query_1")) // Missing query_2

        val result = matcher.determineRequestedDocuments(credentials, credentialSets, availableWalletIds)

        assertEquals(2, result.size)
        assertTrue(result.containsKey(QueryId("query_0")))
        assertTrue(result.containsKey(QueryId("query_1")))
    }

    /**
     * Test based on credentials_sets_optional_only.json
     * Scenario: The request's credential_sets only asks for an optional 'query_1'.
     * A 'query_0' is also defined in the top-level 'credentials' list, but is not
     * mentioned in the credential_sets.
     * The wallet has 'query_0', but is missing the optionally requested 'query_1'.
     * Expected Result: The matcher must return an empty map. 'query_0' is ignored
     * because it's not specified in the 'credential_sets', and 'query_1' is not
     * available to satisfy the optional set.
     */
    @Test
    fun `credential not in credential_sets is ignored, should return empty map`() {

        val query0 = createCredentialQuery("query_0")
        val query1 = createCredentialQuery("query_1")
        val credentials = Credentials(listOf(query0, query1))

        val optionalSet = CredentialSetQuery(
            options = listOf(CredentialQueryIds(listOf(QueryId("query_1")))),
            required = false
        )
        val credentialSets = CredentialSets(listOf(optionalSet))

        // Wallet has query_0, but this query is NOT in the credential_sets.
        // Wallet is missing query_1, so the optional set cannot be satisfied.
        val availableWalletIds = setOf(QueryId("query_0"))

        val result = matcher.determineRequestedDocuments(credentials, credentialSets, availableWalletIds)

        assertTrue(result.isEmpty(), "Expected an empty map because query_0 is not whitelisted and query_1 is not available.")
    }
}