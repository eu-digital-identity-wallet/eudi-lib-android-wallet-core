package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSetQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSets
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.QueryId

class CredentialSetsMatcher {

    /**
     * Determines the final map of requested documents based on the DCQL query and
     * the credentials available in the user's wallet.
     *
     * @param credentials The list of all possible credentials defined in the request.
     * @param credentialSets The credential_sets rules from the request, which may be null.
     * @param availableWalletCredentialIds A set of QueryIds for credentials the wallet actually has.
     * @return A map of QueryId to the corresponding CredentialQuery for documents that should be presented.
     * - If all required credential sets can be satisfied, it returns the required documents
     * plus any optional ones that can also be satisfied.
     * - If any required set cannot be satisfied, it returns an empty map.
     * - If 'credential_sets' is not present, it returns all credentials from the query
     * that are available in the wallet.
     */
    fun determineRequestedDocuments(
        credentials: Credentials,
        credentialSets: CredentialSets?,
        availableWalletCredentialIds: Set<QueryId>
    ): Map<QueryId, CredentialQuery> {
        // Create a lookup map for quick access to credential details by their ID.
        val credentialsMap = credentials.value.associateBy { it.id }

        // The 'credential_sets' array is missing or empty
        if (credentialSets == null || credentialSets.value.isEmpty()) {
            return credentialsMap.filterKeys { it in availableWalletCredentialIds }
        }

        // The 'credential_sets' array is present. Process the new logic.
        val (requiredSets, optionalSets) = credentialSets.value.partition { it.required ?: true }
        val satisfyingDocs = mutableSetOf<QueryId>()

        // Verify all required sets can be satisfied
        for (set in requiredSets) {
            // Find the first option in the set that the wallet can fully satisfy.
            val satisfyingOption = findSatisfyingOption(set, availableWalletCredentialIds)

            if (satisfyingOption != null) {
                // If a valid option is found, add its credentials to our results.
                satisfyingDocs.addAll(satisfyingOption)
            } else {
                // CRITICAL RULE: If even one required set cannot be satisfied,
                // the wallet must not return any credentials at all.
                println("Error: Cannot satisfy a required credential set. Aborting.")
                return emptyMap()
            }
        }

        // Add any optional sets that can be satisfied
        for (set in optionalSets) {
            val satisfyingOption = findSatisfyingOption(set, availableWalletCredentialIds)
            if (satisfyingOption != null) {
                satisfyingDocs.addAll(satisfyingOption)
            }
        }

        // Build the final map from the collected document IDs ---
        return credentialsMap.filterKeys { it in satisfyingDocs }
    }

    /**
     * Helper function to find the first valid "option" within a CredentialSetQuery.
     * An option is valid if the wallet holds all credentials listed in it.
     *
     * @param credentialSet The set to check.
     * @param availableWalletCredentialIds The credentials the user has.
     * @return The list of QueryIds for the first matching option, or null if no option can be satisfied.
     */
    private fun findSatisfyingOption(
        credentialSet: CredentialSetQuery,
        availableWalletCredentialIds: Set<QueryId>
    ): List<QueryId>? {
        // credentialSet.options is a List<CredentialQueryIds>. We need to find the first
        // CredentialQueryIds object where the wallet has all the IDs in its inner list (.value).
        return credentialSet.options.firstOrNull { option ->
            // An option is satisfied if the wallet contains ALL of the IDs in that option's list.
            availableWalletCredentialIds.containsAll(option.value)
        }?.value // Return the inner List<QueryId> of the matching option.
    }
}